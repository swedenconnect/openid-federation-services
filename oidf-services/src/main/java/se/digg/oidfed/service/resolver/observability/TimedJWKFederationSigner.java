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
package se.digg.oidfed.service.resolver.observability;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import se.digg.oidfed.common.jwt.FederationSigner;

/**
 * Federation signer with time keeping.
 *
 * @author Felix Hellman
 */
public class TimedJWKFederationSigner implements FederationSigner {
  private final FederationSigner signer;
  private final ObservationRegistry registry;

  /**
   * Constructor.
   *
   * @param signer   to use
   * @param registry to use
   */
  public TimedJWKFederationSigner(final FederationSigner signer, final ObservationRegistry registry) {
    this.signer = signer;
    this.registry = registry;
  }

  @Override
  public SignedJWT sign(final JOSEObjectType type, final JWTClaimsSet claims) throws JOSEException, ParseException {
    final Observation observation = Observation.createNotStarted("sign", this.registry)
        .lowCardinalityKeyValue("type", type.getType());
    try {
      return observation.observe(() -> {
        try {
          return this.signer.sign(type, claims);
        } catch (final JOSEException | ParseException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof ParseException) {
        throw new ParseException("Failed to parse", e.getCause());
      }
      if (e.getCause() instanceof JOSEException) {
        throw new JOSEException(e.getCause());
      }
      throw e;
    }

  }
}
