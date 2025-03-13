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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.micrometer.core.instrument.DistributionSummary;
import se.digg.oidfed.common.jwt.FederationSigner;
import se.digg.oidfed.common.jwt.JWKFederationSigner;
import se.digg.oidfed.common.jwt.SignerFactory;

import java.time.Clock;

/**
 * Signer factory for creating timed signers.
 *
 * @author Felix Hellman
 */
public class TimedSignerFactory implements SignerFactory {
  private final SignerFactory inner;
  private final Clock clock;
  private final DistributionSummary distributionSummary;

  /**
   * @param inner signer
   * @param clock for time keeping
   * @param distributionSummary for reporting times
   */
  public TimedSignerFactory(
      final SignerFactory inner,
      final Clock clock,
      final DistributionSummary distributionSummary) {
    this.inner = inner;
    this.clock = clock;
    this.distributionSummary = distributionSummary;
  }

  @Override
  public FederationSigner createSigner() {
    return new TimedJWKFederationSigner(this.inner.createSigner(), this.clock, this.distributionSummary);
  }

  @Override
  public JWK getSignKey() {
    return this.inner.getSignKey();
  }

  @Override
  public JWKSet getSignKeys() {
    return this.inner.getSignKeys();
  }
}
