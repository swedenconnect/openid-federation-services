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
package se.digg.oidfed.service.trustmarkissuer;

import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkIssuerProperties;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;
import se.digg.oidfed.trustmarkissuer.TrustMarkSigner;

import java.time.Clock;

/**
 * Factory class for creating trust mark issuers.
 *
 * @author Felix Hellman
 */
public class TrustMarkIssuerFactory {

  private final TrustMarkSigner signer;
  private final CompositeRecordSource source;
  private final Clock clock;

  /**
   * @param signer to use
   * @param source to use
   * @param clock  to use
   */
  public TrustMarkIssuerFactory(
      final TrustMarkSigner signer,
      final CompositeRecordSource source,
      final Clock clock) {
    this.signer = signer;
    this.source = source;
    this.clock = clock;
  }

  /**
   * Creates new instance of a TrustMarkIssuer
   *
   * @param properties to create instance from
   * @return new instance
   */
  public TrustMarkIssuer create(final TrustMarkIssuerProperties properties) {
    return new TrustMarkIssuer(properties, this.signer, this.source, this.clock);
  }
}
