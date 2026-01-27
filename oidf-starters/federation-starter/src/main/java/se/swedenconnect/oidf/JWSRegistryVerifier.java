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
package se.swedenconnect.oidf;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import se.swedenconnect.oidf.common.entity.entity.RecordVerificationException;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryVerifier;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.NotificationRecord;
import se.swedenconnect.oidf.common.entity.validation.FederationAssert;
import se.swedenconnect.oidf.common.entity.entity.integration.JsonRegistryLoader;

import java.security.Key;
import java.text.ParseException;
import java.util.List;

/**
 * JWS implementation of {@link RegistryVerifier}.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class JWSRegistryVerifier implements RegistryVerifier {

  private final JWKSet validationKeys;
  private final JsonRegistryLoader loader;

  @Override
  public Expirable<List<EntityRecord>> verifyEntityRecords(final String jwt) {
    try {
      final JWTClaimsSet claims = this.verify(jwt)
          .getJWTClaimsSet();

      final String entityJson = claims.getClaimAsString("entity_records");

      final List<EntityRecord> entities = this.loader.parseEntityRecord(entityJson)
          .stream()
          .toList();
      return new Expirable<>(claims.getExpirationTime().toInstant(), claims.getIssueTime().toInstant(), entities);
    } catch (final ParseException | JOSEException e) {
      throw new RecordVerificationException("Failed to verify entity record", e);
    }
  }

  @Override
  public Expirable<ModuleRecord> verifyModuleResponse(final String jwt) {
    try {
      final SignedJWT signedJWT = this.verify(jwt);
      final JWTClaimsSet claims = signedJWT
          .getJWTClaimsSet();
      final String json = claims.getClaimAsString("module_records");
      FederationAssert.assertNotEmpty(json, "Missing claim for:'module_records'");

      final ModuleRecord moduleRecord = this.loader.parseModuleJson(json);
      FederationAssert.assertNotEmpty(claims.getExpirationTime(), "Missing claim 'exp' in token");

      return new Expirable<>(claims.getExpirationTime().toInstant(), claims.getIssueTime().toInstant(), moduleRecord);
    } catch (final ParseException | JOSEException e) {
      throw new RecordVerificationException("Failed to verify module record", e);
    }
  }

  @Override
  public NotificationRecord verifyNotification(final String jwt) {
    try {
      this.verify(jwt);
      return new NotificationRecord();
    } catch (final ParseException | JOSEException e) {
      throw new RecordVerificationException("Failed to verify TrustMarkIssuerSubject record", e);
    }
  }

  private SignedJWT verify(final String jwtString) throws JOSEException, ParseException {
    final SignedJWT jwt = SignedJWT.parse(jwtString);
    final Key key = this.selectKey(jwt);

    final JWSVerifier jwsVerifier = new DefaultJWSVerifierFactory()
        .createJWSVerifier(jwt.getHeader(), key);

    if (!jwt.verify(jwsVerifier)) {
      throw new RecordVerificationException("Failed to verify signature on record");
    }
    return jwt;
  }


  protected Key selectKey(final SignedJWT jwt) throws JOSEException {
    final JWKSelector selector = new JWKSelector(new JWKMatcher.Builder()
        .keyID(jwt.getHeader().getKeyID())
        .build());

    final JWK jwk = selector
        .select(this.validationKeys)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unable to resolve key for JWT with kid:'%s' "
            .formatted(jwt.getHeader().getKeyID())));

    return switch (jwk.getKeyType().getValue()) {
      case "EC" -> jwk.toECKey().toKeyPair().getPublic();
      case "RSA" -> jwk.toRSAKey().toKeyPair().getPublic();
      case null, default -> throw new IllegalArgumentException("Unsupported key type");
    };
  }
}
