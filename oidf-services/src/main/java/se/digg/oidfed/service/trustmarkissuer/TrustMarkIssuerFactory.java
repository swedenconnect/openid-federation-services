/*
 * Copyright 2024 Sweden Connect
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

import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuerProperties;
import se.digg.oidfed.trustmarkissuer.TrustMarkSigner;
import se.digg.oidfed.trustmarkissuer.TrustMarkSubjectRepository;

/**
 * Factory class for creating trust mark issuers.
 *
 * @author Felix Hellman
 */
public class TrustMarkIssuerFactory {

  private final TrustMarkSigner signer;
  private final TrustMarkSubjectRepository repository;

  /**
   * @param signer     to use
   * @param repository to use
   */
  public TrustMarkIssuerFactory(final TrustMarkSigner signer, final TrustMarkSubjectRepository repository) {
    this.signer = signer;
    this.repository = repository;
  }

  /**
   * Creates new instance of a TrustMarkIssuer
   *
   * @param properties to create instance from
   * @return new instance
   */
  public TrustMarkIssuer create(
      final TrustMarkIssuerProperties properties) {
    return new TrustMarkIssuer(properties, this.signer, this.repository);
  }
}
