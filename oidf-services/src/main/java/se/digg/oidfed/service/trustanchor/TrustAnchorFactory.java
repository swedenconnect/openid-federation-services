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
package se.digg.oidfed.service.trustanchor;

import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
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
  private final SubordinateStatementFactory factory;

  /**
   * Constructor.
   * @param registry to use
   * @param factory to use
   */
  public TrustAnchorFactory(final EntityRecordRegistry registry, final SubordinateStatementFactory factory) {
    this.registry = registry;
    this.factory = factory;
  }

  /**
   * @param properties for trust anchor
   * @return new instance
   */
  public TrustAnchor create(final TrustAnchorProperties properties) {
    return new TrustAnchor(this.registry, properties, this.factory);
  }
}
