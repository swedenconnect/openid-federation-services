/*
 * Copyright 2024-2026 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.resolver;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class TestEntitiesFactory {

  public static final String TA_ID = "https://ta.example.com";
  public static final String IM_ID = "https://im.example.com";
  public static final String LEAF_ID = "https://leaf.example.com";
  public static final String SAML_SP_ID = "https://saml-sp.example.com";

  public final JWK taKey;
  public final JWK imKey;
  public final JWK leafKey;
  public final JWK samlSpKey;

  public final SignedJWT taEC;
  public final SignedJWT imEC;
  public final SignedJWT leafEC;
  public final SignedJWT samlSpEC;

  public final SignedJWT taImSub;
  public final SignedJWT imLeafSub;
  public final SignedJWT imSamlSpSub;

  public TestEntitiesFactory() throws Exception {
    taKey = new RSAKeyGenerator(2048).keyID("ta-key").generate();
    imKey = new RSAKeyGenerator(2048).keyID("im-key").generate();
    leafKey = new RSAKeyGenerator(2048).keyID("leaf-key").generate();
    samlSpKey = new RSAKeyGenerator(2048).keyID("saml-sp-key").generate();

    taEC = buildSelfStatement(taKey, TA_ID, taMetadata(), null);
    imEC = buildSelfStatement(imKey, IM_ID, imMetadata(), List.of(TA_ID));
    leafEC = buildSelfStatement(leafKey, LEAF_ID, leafMetadata(), List.of(IM_ID));
    samlSpEC = buildSelfStatement(samlSpKey, SAML_SP_ID, samlSpMetadata(), List.of(IM_ID));

    taImSub = buildSubordinateStatement(taKey, TA_ID, IM_ID, imKey.toPublicJWK());
    imLeafSub = buildSubordinateStatement(imKey, IM_ID, LEAF_ID, leafKey.toPublicJWK());
    imSamlSpSub = buildSubordinateStatement(imKey, IM_ID, SAML_SP_ID, samlSpKey.toPublicJWK());
  }

  private static SignedJWT buildSelfStatement(
      final JWK key,
      final String entityId,
      final JSONObject metadata,
      final List<String> authorityHints) throws Exception {

    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(key.getKeyID())
        .build();

    final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
        .issuer(entityId)
        .subject(entityId)
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plus(Duration.ofDays(1))))
        .claim("jwks", new JSONObject(new JWKSet(key.toPublicJWK()).toJSONObject()))
        .claim("metadata", metadata);

    if (authorityHints != null) {
      claims.claim("authority_hints", authorityHints);
    }

    final SignedJWT jwt = new SignedJWT(header, claims.build());
    jwt.sign(new RSASSASigner(key.toRSAKey()));
    return jwt;
  }

  private static SignedJWT buildSubordinateStatement(
      final JWK signingKey,
      final String issuer,
      final String subject,
      final JWK subjectPublicKey) throws Exception {

    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(signingKey.getKeyID())
        .build();

    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(subject)
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plus(Duration.ofDays(1))))
        .claim("jwks", new JSONObject(new JWKSet(subjectPublicKey).toJSONObject()))
        .build();

    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new RSASSASigner(signingKey.toRSAKey()));
    return jwt;
  }

  private static JSONObject taMetadata() {
    final JSONObject federationEntity = new JSONObject();
    federationEntity.put("federation_fetch_endpoint", TA_ID + "/fetch");
    federationEntity.put("federation_list_endpoint", TA_ID + "/list");
    federationEntity.put("federation_resolve_endpoint", TA_ID + "/resolve");
    federationEntity.put("organization_name", "Test Trust Anchor");
    final JSONObject metadata = new JSONObject();
    metadata.put("federation_entity", federationEntity);
    return metadata;
  }

  private static JSONObject imMetadata() {
    final JSONObject federationEntity = new JSONObject();
    federationEntity.put("federation_fetch_endpoint", IM_ID + "/fetch");
    federationEntity.put("federation_list_endpoint", IM_ID + "/list");
    federationEntity.put("organization_name", "Test Intermediate");
    final JSONObject metadata = new JSONObject();
    metadata.put("federation_entity", federationEntity);
    return metadata;
  }

  private static JSONObject leafMetadata() {
    final JSONObject rp = new JSONObject();
    rp.put("redirect_uris", List.of("https://leaf.example.com/callback"));
    rp.put("organization_name", "Test Leaf");
    final JSONObject metadata = new JSONObject();
    metadata.put("openid_relying_party", rp);
    return metadata;
  }

  private static JSONObject samlSpMetadata() {
    final JSONObject acs = new JSONObject();
    acs.put("binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
    acs.put("location", SAML_SP_ID + "/sso/post");
    acs.put("index", 1);
    acs.put("is_default", true);

    final JSONObject slo = new JSONObject();
    slo.put("binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
    slo.put("location", SAML_SP_ID + "/slo/post");

    final JSONObject sp = new JSONObject();
    sp.put("organization_name", "Test SAML SP");
    sp.put("organization_display_name", "Test SAML SP");
    sp.put("authn_requests_signed", true);
    sp.put("want_assertions_signed", true);
    sp.put("assertion_consumer_services", List.of(acs));
    sp.put("single_logout_services", List.of(slo));
    sp.put("name_id_formats", List.of("urn:oasis:names:tc:SAML:2.0:nameid-format:transient"));
    sp.put("entity_categories", List.of(
        "http://id.elegnamnden.se/ec/1.0/loa3-pnr",
        "http://id.elegnamnden.se/st/1.0/public-sector-sp"
    ));

    final JSONObject federationEntity = new JSONObject();
    federationEntity.put("organization_name", "Test SAML SP");

    final JSONObject metadata = new JSONObject();
    metadata.put("federation_entity", federationEntity);
    metadata.put("saml_service_provider", sp);
    return metadata;
  }
}
