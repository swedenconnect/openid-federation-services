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
package se.swedenconnect.oidf.resolver.chain;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests for {@link ChainValidator}.
 *
 * @author Felix Hellman
 */
class ChainValidatorTest {

  @Test
  void allStepsAreExecutedAndAllErrorsReported() {
    final ChainValidationStep failing = chain -> {
      throw new IllegalStateException("step broke");
    };
    final ChainValidationStep invalid = chain -> List.of(
        new ChainValidationError(ChainValidationErrorType.LEAF_EXPIRED, "first", null),
        new ChainValidationError(ChainValidationErrorType.STATEMENT_EXPIRED, "second", null));
    final SignedJWT statement = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),
        new JWTClaimsSet.Builder().issuer("https://leaf").subject("https://leaf").build());

    final ChainValidationResult result = new ChainValidator(List.of(failing, invalid)).validate(List.of(statement));

    Assertions.assertEquals(List.of(
            ChainValidationErrorType.CHAIN_TOO_SHORT,
            ChainValidationErrorType.VALIDATION_STEP_FAILED,
            ChainValidationErrorType.LEAF_EXPIRED,
            ChainValidationErrorType.STATEMENT_EXPIRED),
        result.typedErrors().stream().map(ChainValidationError::getErrorType).toList());
    Assertions.assertEquals(1, result.errors().size());
    Assertions.assertInstanceOf(ChainValidationFailedException.class, result.errors().getFirst());
  }
}
