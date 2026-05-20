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
package se.swedenconnect.oidf.routing;

import org.springframework.web.servlet.function.ServerRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

/**
 * Router interface for module-specific request handling.
 *
 * @author Felix Hellman
 */
public interface ModuleRouter {
  /**
   * @param request incoming request
   * @param entity  the matched entity record
   * @return the response to return
   */
  CachedResponse handleRequest(ServerRequest request, EntityRecord entity);

  /**
   * @param request incoming request
   * @param entity  the candidate entity record
   * @return true if this router will handle the request for the given entity
   */
  boolean willHandleRequest(ServerRequest request, EntityRecord entity);
}
