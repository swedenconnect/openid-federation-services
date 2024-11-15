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
package se.digg.oidfed.resolver;

import se.digg.oidfed.resolver.tree.EntityStatementTree;

/**
 * Responsible for discovery.
 *
 * @author Felix Hellman
 */
public class Discovery {

  private final EntityStatementTree tree;

  /**
   * @param tree to search
   */
  public Discovery(final EntityStatementTree tree) {
    this.tree = tree;
  }

  /**
   * @param request to process
   * @return discovery response
   */
  public DiscoveryResponse discovery(final DiscoveryRequest request) {
    return new DiscoveryResponse(tree.discovery(request));
  }
}
