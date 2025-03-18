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
package se.digg.oidfed.service.cache.managed;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.digg.oidfed.service.submodule.RequestResponseEntry;
import se.digg.oidfed.service.submodule.RequestResponseModuleCache;

import java.util.Set;

/**
 * No Operation implementation of {@link RequestResponseCacheFactory}
 *
 * @author Felix Hellman
 */
public class NoopRequestResponseCacheFactory implements RequestResponseCacheFactory {
  @Override
  public RequestResponseModuleCache create(final EntityID entityID) {
    //Implements a NO-OP cache
    return new RequestResponseModuleCache() {
      @Override
      public void add(final RequestResponseEntry requestResponseEntry) {

      }

      @Override
      public Set<String> flushRequestKeys() {
        return Set.of();
      }

      @Override
      public RequestResponseEntry get(final String key) {
        return null;
      }
    };
  }
}
