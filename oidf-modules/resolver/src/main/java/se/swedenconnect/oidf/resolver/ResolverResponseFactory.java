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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.ParseException;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Factory class responsible for constructing resolver responses.
 *
 * @author Felix Hellman
 */
public class ResolverResponseFactory {
  private final Clock clock;
  private final ResolverProperties properties;
  private final SignerFactory signerFactory;
  private final CompositeRecordSource compositeRecordSource;

  /**
   * Constructor.
   *
   * @param clock         for determining current time
   * @param properties    for response parameters
   * @param signerFactory for creating a signer
   * @param compositeRecordSource for finding entity of resolver
   */
  public ResolverResponseFactory(
      final Clock clock,
      final ResolverProperties properties,
      final SignerFactory signerFactory,
      final CompositeRecordSource compositeRecordSource) {
    this.clock = clock;
    this.properties = properties;
    this.signerFactory = signerFactory;
    this.compositeRecordSource = compositeRecordSource;
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
            .issuer(this.properties.getEntityIdentifier())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(Optional.ofNullable(this.properties.getResolveResponseDuration())
                .orElse(Duration.ofDays(7)))))
            .claim("metadata", resolverResponse.metadata())
            .claim("trust_marks",
                resolverResponse.trustMarkEntries().stream().map(trustMark -> {
                      final String jwt = trustMark.getTrustMark().serialize();
                      return Map.of("trust_mark", jwt, "trust_mark_type", trustMark.getID().getValue());
                    })
                    .toList())
            .claim("trust_chain",
                resolverResponse.trustChain().stream().map(statement -> statement.getSignedStatement().serialize())
                    .toList())
            .build();
    return this.signerFactory.createSigner(this.compositeRecordSource.getEntity(new NodeKey(
            this.properties.getEntityIdentifier(), this.properties.getEntityIdentifier()
        )).get())
        .sign(new JOSEObjectType("resolve-response+jwt"), claims)
        .serialize();
  }
}
