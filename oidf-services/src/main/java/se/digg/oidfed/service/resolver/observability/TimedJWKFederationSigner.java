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
import io.micrometer.core.instrument.DistributionSummary;
import se.digg.oidfed.common.jwt.FederationSigner;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Federation signer with time keeping.
 *
 * @author Felix Hellman
 */
public class TimedJWKFederationSigner implements FederationSigner {
  private final FederationSigner signer;
  private final Clock clock;
  private final DistributionSummary register;

  /**
   * Constructor.
   *
   * @param signer   to use
   * @param clock    to use
   * @param register to use
   */
  public TimedJWKFederationSigner(
      final FederationSigner signer,
      final Clock clock,
      final DistributionSummary register) {
    this.signer = signer;
    this.clock = clock;
    this.register = register;
  }

  @Override
  public SignedJWT sign(final JOSEObjectType type, final JWTClaimsSet claims) throws JOSEException, ParseException {
    final Instant start = Instant.now(this.clock);
    final SignedJWT sign = this.signer.sign(type, claims);
    final long duration = Duration.between(start, Instant.now(this.clock)).toMillis();
    this.register.record(duration);
    return sign;
  }
}
