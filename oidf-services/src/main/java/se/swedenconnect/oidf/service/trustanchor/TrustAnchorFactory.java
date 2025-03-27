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
package se.swedenconnect.oidf.service.trustanchor;

import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;
import se.swedenconnect.oidf.trustanchor.SubordinateStatementFactory;
import se.swedenconnect.oidf.trustanchor.TrustAnchor;

/**
 * Factory class for creating trust anchors.
 *
 * @author Felix Hellman
 */
public class TrustAnchorFactory {

  private final CompositeRecordSource source;
  private final SignerFactory signerFactory;
  private final FederationClient client;

  /**
   * Constructor.
   *
   * @param source        to use
   * @param signerFactory to use
   * @param client        to use
   */
  public TrustAnchorFactory(
      final CompositeRecordSource source,
      final SignerFactory signerFactory,
      final FederationClient client
  ) {
    this.source = source;
    this.signerFactory = signerFactory;
    this.client = client;
  }

  /**
   * @param properties for trust anchor
   * @return new instance
   */
  public TrustAnchor create(
      final TrustAnchorProperties properties) {
    return new TrustAnchor(this.source, properties,
        new SubordinateStatementFactory(this.signerFactory, properties),
        this.client
    );
  }
}
