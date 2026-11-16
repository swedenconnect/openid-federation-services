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
package se.swedenconnect.oidf.trustmarkissuer.starter;

import se.swedenconnect.oidf.LocalFederationEndpoints;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.trustmarkissuer.TrustMarkStatusRequest;

import java.util.Optional;

/**
 * The trust mark status endpoints of the Trust Mark Issuers hosted by this service, answered the same way as
 * {@code TrustMarkIssuerRouter} answers them over HTTP.
 *
 * @author Felix Hellman
 */
public class TrustMarkIssuerLocalEndpoints implements LocalFederationEndpoints {

  private final CompositeRecordSource source;
  private final TrustMarkIssuerFactory factory;

  /**
   * Constructor.
   *
   * @param source  of the Trust Mark Issuer properties
   * @param factory for the Trust Mark Issuers
   */
  public TrustMarkIssuerLocalEndpoints(final CompositeRecordSource source, final TrustMarkIssuerFactory factory) {
    this.source = source;
    this.factory = factory;
  }

  @Override
  public Optional<String> trustMarkStatus(final String issuerId, final String trustMark) throws Exception {
    final Optional<TrustMarkIssuerProperties> properties = this.source.getTrustMarkIssuerProperties().stream()
        .filter(issuer -> issuer.entityIdentifier().getValue().equals(issuerId))
        .findFirst();
    if (properties.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(this.factory.create(properties.get()).trustMarkStatus(new TrustMarkStatusRequest(trustMark)));
  }
}
