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
package se.digg.oidfed.service.trustanchor;

import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.RecordRegistrySource;
import se.digg.oidfed.common.jwt.SignerFactory;
import se.digg.oidfed.trustanchor.SubordinateStatementFactory;
import se.digg.oidfed.trustanchor.TrustAnchor;
import se.digg.oidfed.trustanchor.TrustAnchorProperties;

/**
 * Factory class for creating trust anchors.
 *
 * @author Felix Hellman
 */
public class TrustAnchorFactory {

  private final EntityRecordRegistry registry;
  private final RecordRegistrySource source;
  private final SignerFactory signerFactory;
  private final RestClient client;

  /**
   * Constructor.
   *
   * @param registry to use
   * @param source   to use
   * @param signerFactory to use
   * @param client to use
   */
  public TrustAnchorFactory(
      final EntityRecordRegistry registry,
      final RecordRegistrySource source,
      final SignerFactory signerFactory,
      final RestClient client) {
    this.registry = registry;
    this.source = source;
    this.signerFactory = signerFactory;
    this.client = client;
  }

  /**
   * @param properties for trust anchor
   * @return new instance
   */
  public TrustAnchor create(final TrustAnchorProperties properties) {
    return new TrustAnchor(this.registry, properties, new SubordinateStatementFactory(this.source, this.signerFactory,
        properties.getBasePath()), new RestClientEntityConfigurationLoader(this.client));
  }
}
