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
package se.swedenconnect.oidf.routing;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;

import java.net.URI;

/**
 * Router interface for adding routes.
 *
 * @author Felix Hellman
 */
public interface Router {
  /**
   * @param source to read from
   * @param route to add routes to
   */
  void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route);

  /**
   * @param serverRequest to find entity id for
   * @return entity id
   */
  default EntityID getEntityIdFromReuqest(final ServerRequest serverRequest) {
    final URI uri = serverRequest.uri();
    final String withoutQuery = uri.getScheme() + "://" + uri.getAuthority() + uri.getPath();
    return new EntityID(withoutQuery.substring(0, withoutQuery.lastIndexOf('/')));
  }
}
