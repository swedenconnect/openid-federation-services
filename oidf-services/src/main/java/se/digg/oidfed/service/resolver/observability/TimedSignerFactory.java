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
import io.micrometer.observation.ObservationRegistry;
import se.digg.oidfed.common.jwt.FederationSigner;
import se.digg.oidfed.common.jwt.SignerFactory;

/**
 * Signer factory for creating timed signers.
 *
 * @author Felix Hellman
 */
public class TimedSignerFactory implements SignerFactory {
  private final SignerFactory inner;
  private final ObservationRegistry registry;


  /**
   * @param inner    signer
   * @param registry to use
   */
  public TimedSignerFactory(final SignerFactory inner, final ObservationRegistry registry) {
    this.inner = inner;
    this.registry = registry;
  }

  @Override
  public FederationSigner createSigner() {
    return new TimedJWKFederationSigner(this.inner.createSigner(), this.registry);
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
