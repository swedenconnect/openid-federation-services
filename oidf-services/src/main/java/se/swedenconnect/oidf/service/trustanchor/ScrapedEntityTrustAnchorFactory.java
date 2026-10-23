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
package se.swedenconnect.oidf.service.trustanchor;

import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;
import se.swedenconnect.oidf.resolver.ResolverCacheRegistry;
import se.swedenconnect.oidf.trustanchor.DefaultTrustAnchor;
import se.swedenconnect.oidf.trustanchor.SubordinateStatementFactory;
import se.swedenconnect.oidf.trustanchor.TrustAnchor;
import se.swedenconnect.oidf.trustanchor.TrustAnchorFactory;

/**
 * Trust anchor factory that reads subordinate entity configurations from a resolver with the same trust anchor.
 *
 * @author Felix Hellman
 */
public class ScrapedEntityTrustAnchorFactory extends TrustAnchorFactory {

  private final CompositeRecordSource source;
  private final SignerFactory signerFactory;
  private final FederationClient client;
  private final ResolverCacheRegistry registry;

  /**
   * Constructor.
   *
   * @param source        to use
   * @param signerFactory to use
   * @param client        to use when no scraped entity is available
   * @param registry      of resolver trees
   */
  public ScrapedEntityTrustAnchorFactory(
      final CompositeRecordSource source,
      final SignerFactory signerFactory,
      final FederationClient client,
      final ResolverCacheRegistry registry
  ) {
    super(source, signerFactory, client);
    this.source = source;
    this.signerFactory = signerFactory;
    this.client = client;
    this.registry = registry;
  }

  @Override
  public TrustAnchor create(final TrustAnchorProperties properties) {
    final String trustAnchorId = properties.getEntityIdentifier().getValue();
    final FederationClient trustAnchorClient = this.registry.getAllRegistrations().stream()
        .filter(registration -> trustAnchorId.equals(registration.properties().getTrustAnchor()))
        .findFirst()
        .<FederationClient>map(registration ->
            new ScrapedEntityFederationClient(registration.tree().getCurrentSnapshot(), this.client))
        .orElse(this.client);
    return new DefaultTrustAnchor(this.source, properties,
        new SubordinateStatementFactory(this.signerFactory),
        trustAnchorClient
    );
  }
}
