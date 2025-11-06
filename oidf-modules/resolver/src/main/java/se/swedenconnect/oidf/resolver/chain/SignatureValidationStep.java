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
package se.swedenconnect.oidf.resolver.chain;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates the signatures of the chain.
 *
 * @author Felix Hellman
 */
public class SignatureValidationStep implements ChainValidationStep {

  private final JWKSet trustedKeys;

  /**
   * Constructor.
   *
   * @param trustedKeys for verification of TrustAnchor signature.
   */
  public SignatureValidationStep(final JWKSet trustedKeys) {
    this.trustedKeys = trustedKeys;
  }

  @Override
  public List<ChainValidationError> validate(final List<EntityStatement> chain) {
    final ArrayList<ChainValidationError> errors = new ArrayList<>();
    
    //Verify leaf
    final EntityStatement leaf = chain.getFirst();
    try {
      Objects.requireNonNull(leaf.verifySignatureOfSelfStatement());
    } catch (final Exception e) {
      errors.add(new ChainValidationError("Invalid leaf statement", e));
    }
    try {
      verifyValidityTime(leaf);
    } catch (final Exception e) {
      errors.add(new ChainValidationError("Leaf validity time has expired", e));
    }
    //Verify TA
    try {
      this.verifyEntity(chain.getLast());
    } catch (final Exception e) {
      errors.add(new ChainValidationError("TA is not valid", e));
    }

    for (int i = 0; i < chain.size() - 1; i++) {
      final EntityStatement current = chain.get(i);
      final EntityStatement next = chain.get(i + 1);
      try {
        verifyLink(current, next);
      } catch (final Exception e) {
        errors.add(new ChainValidationError("Failed to verify link between %s and %s"
            .formatted(current.getEntityID(), next.getEntityID()), e));
      }
      try {
        verifyValidityTime(current);
      } catch (final Exception e) {
        errors.add(new ChainValidationError("Failed to verify validity time of %s".formatted(current.getEntityID()), e));
      }
    }

    return errors;
  }

  private static void verifyLink(final EntityStatement current, final EntityStatement next)
      throws BadJOSEException, JOSEException {
    final String currentIssuer = current.getClaimsSet().getIssuer().getValue();
    final String nextSubject = next.getClaimsSet().getSubject().getValue();
    if (!currentIssuer.equals(nextSubject)) {
      throw new IllegalArgumentException(
          "Current issuer:%s is not same as next subject:%s".formatted(currentIssuer, nextSubject)
      );
    }
    current.verifySignature(next.getClaimsSet().getJWKSet());
  }

  private void verifyEntity(final EntityStatement entity) throws BadJOSEException, JOSEException {
    entity.verifySignature(this.trustedKeys);
    // Verify that TA is selfsigned
    entity.verifySignature(entity.getClaimsSet().getJWKSet());
    // Verify validity time
    verifyValidityTime(entity);
  }

  private static void verifyValidityTime(final EntityStatement entityStatement) {

    if (entityStatement.getClaimsSet().getIssueTime() == null) {
      throw new IllegalArgumentException("Entity Statement has no issue time");
    }

    if (entityStatement.getClaimsSet().getExpirationTime() == null) {
      throw new IllegalArgumentException("Entity Statement has no expiration time");
    }

    final Instant issueTime = Instant.ofEpochMilli(entityStatement.getClaimsSet().getIssueTime().getTime());
    if (Instant.now().isBefore(issueTime.minusSeconds(15))) {
      throw new IllegalArgumentException("Entity Statement issue time is in the future");
    }

    final Instant expirationTime = Instant.ofEpochMilli(entityStatement.getClaimsSet().getExpirationTime().getTime());
    if (Instant.now().isAfter(expirationTime)) {
      throw new IllegalArgumentException("Entity Statement has expired");
    }
  }
}
