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
package se.digg.oidfed.resolver;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.ParseException;
import se.digg.oidfed.common.entity.integration.registry.ResolverProperties;
import se.digg.oidfed.common.jwt.SignerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Factory class responsible for constructing resolver responses.
 *
 * @author Felix Hellman
 */
public class ResolverResponseFactory {
  private final Clock clock;
  private final ResolverProperties properties;
  private final SignerFactory signerFactory;

  private final Map<KeyType, JWSAlgorithm> keyTypeJWSAlgorithmMap = Map.of(
      KeyType.EC, JWSAlgorithm.ES256,
      KeyType.RSA, JWSAlgorithm.RS256
  );

  /**
   * Constructor.
   *
   * @param clock         for determining current time
   * @param properties    for response parameters
   * @param signerFactory for finding a signer
   */
  public ResolverResponseFactory(
      final Clock clock,
      final ResolverProperties properties,
      final SignerFactory signerFactory) {
    this.clock = clock;
    this.properties = properties;
    this.signerFactory = signerFactory;
  }

  /**
   * Constructs and signs response for the resolver.
   *
   * @param resolverResponse to create a response for
   * @return signed jwt as string
   * @throws ParseException
   * @throws JOSEException
   */
  public String sign(final ResolverResponse resolverResponse) throws ParseException, JOSEException {
    final Instant now = Instant.now(this.clock);
    final JWTClaimsSet claims =
        new JWTClaimsSet.Builder(resolverResponse.entityStatement().getClaimsSet().toJWTClaimsSet())
            .issuer(this.properties.entityIdentifier())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(this.properties.resolveResponseDuration())))
            .claim("metadata", resolverResponse.metadata())
            .claim("trust_marks",
                resolverResponse.trustMarkEntries().stream().map(trustMark -> trustMark.getTrustMark().serialize())
                    .toList())
            .claim("trust_chain",
                resolverResponse.trustChain().stream().map(statement -> statement.getSignedStatement().serialize())
                    .toList())
            .build();
    return this.signerFactory.createSigner()
        .sign(new JOSEObjectType("resolve-response+jwt"), claims)
        .serialize();
  }
}
