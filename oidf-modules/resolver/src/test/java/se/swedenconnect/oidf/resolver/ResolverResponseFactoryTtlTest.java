/*
 * Copyright 2024-2025 Sweden Connect
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
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.jwt.FederationSigner;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolverResponseFactoryTtlTest {

  private static final String ENTITY_ID = "https://example.com/resolver";

  @Mock
  private SignerFactory signerFactory;

  @Mock
  private FederationSigner signer;

  @Mock
  private CompositeRecordSource compositeRecordSource;

  @Mock
  private ResolverProperties properties;

  @Mock
  private EntityRecord entityRecord;

  private Clock fixedClock;
  private Instant now;

  @BeforeEach
  void setUp() throws Exception {
    now = Instant.parse("2025-01-01T12:00:00Z");
    fixedClock = Clock.fixed(now, ZoneId.of("UTC"));

    when(properties.getEntityIdentifier()).thenReturn(ENTITY_ID);
    when(properties.getResolveResponseDuration()).thenReturn(null);
    when(compositeRecordSource.getEntity(any(NodeKey.class))).thenReturn(Optional.of(entityRecord));
    when(signerFactory.createSigner(any())).thenReturn(signer);

    final ArgumentCaptor<JOSEObjectType> typeCaptor = ArgumentCaptor.forClass(JOSEObjectType.class);
    final ArgumentCaptor<JWTClaimsSet> claimsCaptor = ArgumentCaptor.forClass(JWTClaimsSet.class);
  }

  @Test
  void ttlIsCappedByShortestEntityStatementExpiry() throws Exception {
    final JWK key = new RSAKeyGenerator(2048).keyID("test-key").generate();

    // Chain with one entity expiring in 1 day
    final Instant shortExpiry = now.plus(Duration.ofDays(1));
    final EntityStatement shortExpiryEs = buildEntityStatement(key, ENTITY_ID, ENTITY_ID, shortExpiry);

    // Chain with another entity expiring in 30 days
    final Instant longExpiry = now.plus(Duration.ofDays(30));
    final EntityStatement longExpiryEs = buildEntityStatement(key, "https://example.com/ta",
        "https://example.com/ta", longExpiry);

    final ArgumentCaptor<JWTClaimsSet> claimsCaptor = ArgumentCaptor.forClass(JWTClaimsSet.class);
    final SignedJWT mockJwt = buildSignedJwt(key, ENTITY_ID, ENTITY_ID, shortExpiry);
    when(signer.sign(any(), claimsCaptor.capture())).thenReturn(mockJwt);

    final ResolverResponseFactory factory = new ResolverResponseFactory(
        fixedClock, properties, signerFactory, compositeRecordSource);

    final ResolverResponse response = ResolverResponse.builder()
        .entityStatement(shortExpiryEs)
        .metadata(null)
        .trustMarkEntries(List.of())
        .trustChain(List.of(shortExpiryEs, longExpiryEs))
        .validationErrors(List.of())
        .build();

    factory.sign(response);

    final JWTClaimsSet capturedClaims = claimsCaptor.getValue();
    final Instant capturedExpiry = capturedClaims.getExpirationTime().toInstant();

    // Should be ~1 day from now, not 7 days (default)
    final Duration actualDuration = Duration.between(now, capturedExpiry);
    Assertions.assertTrue(actualDuration.toDays() <= 1,
        "TTL should be capped at the shortest entity statement expiry (1 day), but was: " + actualDuration.toDays() + " days");
  }

  @Test
  void ttlUsesDefaultWhenNoShortExpiryInChain() throws Exception {
    final JWK key = new RSAKeyGenerator(2048).keyID("test-key").generate();

    // Chain with entities expiring in 30 days (longer than default 7 days)
    final Instant longExpiry = now.plus(Duration.ofDays(30));
    final EntityStatement longExpiryEs = buildEntityStatement(key, ENTITY_ID, ENTITY_ID, longExpiry);

    final ArgumentCaptor<JWTClaimsSet> claimsCaptor = ArgumentCaptor.forClass(JWTClaimsSet.class);
    final SignedJWT mockJwt = buildSignedJwt(key, ENTITY_ID, ENTITY_ID, longExpiry);
    when(signer.sign(any(), claimsCaptor.capture())).thenReturn(mockJwt);

    final ResolverResponseFactory factory = new ResolverResponseFactory(
        fixedClock, properties, signerFactory, compositeRecordSource);

    final ResolverResponse response = ResolverResponse.builder()
        .entityStatement(longExpiryEs)
        .metadata(null)
        .trustMarkEntries(List.of())
        .trustChain(List.of(longExpiryEs))
        .validationErrors(List.of())
        .build();

    factory.sign(response);

    final JWTClaimsSet capturedClaims = claimsCaptor.getValue();
    final Instant capturedExpiry = capturedClaims.getExpirationTime().toInstant();

    final Duration actualDuration = Duration.between(now, capturedExpiry);
    // Default is 7 days, chain has 30 days — should use default (7 days)
    Assertions.assertTrue(actualDuration.toDays() >= 6 && actualDuration.toDays() <= 7,
        "TTL should use default 7 days when chain exp is longer, but was: " + actualDuration.toDays() + " days");
  }

  private EntityStatement buildEntityStatement(final JWK key, final String issuer, final String subject,
                                               final Instant expiry) throws Exception {
    final SignedJWT jwt = buildSignedJwt(key, issuer, subject, expiry);
    return EntityStatement.parse(jwt.serialize());
  }

  private SignedJWT buildSignedJwt(final JWK key, final String issuer, final String subject,
                                   final Instant expiry) throws Exception {
    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(key.getKeyID())
        .build();
    final net.minidev.json.JSONObject federationEntityMetadata = new net.minidev.json.JSONObject();
    federationEntityMetadata.put("organization_name", "Test");
    final net.minidev.json.JSONObject metadata = new net.minidev.json.JSONObject();
    metadata.put("federation_entity", federationEntityMetadata);
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(subject)
        .issueTime(Date.from(now))
        .expirationTime(Date.from(expiry))
        .claim("jwks", new net.minidev.json.JSONObject(
            new com.nimbusds.jose.jwk.JWKSet(key.toPublicJWK()).toJSONObject()))
        .claim("metadata", metadata)
        .build();
    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new RSASSASigner(key.toRSAKey()));
    return jwt;
  }
}
