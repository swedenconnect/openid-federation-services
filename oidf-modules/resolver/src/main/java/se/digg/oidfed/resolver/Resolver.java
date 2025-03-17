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
package se.digg.oidfed.resolver;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.digg.oidfed.common.entity.integration.federation.ResolveRequest;
import se.digg.oidfed.common.exception.FederationException;

/**
 * Resolver interface.
 *
 * @author Felix Hellman
 */
public interface Resolver {
  /**
   * @param request from the resolver api
   * @return response
   * @throws FederationException
   */
  String resolve(final ResolveRequest request) throws FederationException;

  /**
   * @param request to process
   * @return discovery response
   */
  DiscoveryResponse discovery(final DiscoveryRequest request);

  /**
   * @return entity id of this resolver
   */
  EntityID getEntityId();
}
