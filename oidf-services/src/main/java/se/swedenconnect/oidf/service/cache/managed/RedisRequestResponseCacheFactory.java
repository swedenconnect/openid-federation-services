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
package se.swedenconnect.oidf.service.cache.managed;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.data.redis.core.RedisTemplate;
import se.swedenconnect.oidf.service.configuration.OpenIdFederationConfigurationProperties;
import se.swedenconnect.oidf.service.submodule.RedisModuleRequestResponseCache;
import se.swedenconnect.oidf.service.submodule.RequestResponseEntry;
import se.swedenconnect.oidf.service.submodule.RequestResponseModuleCache;

/**
 * Redis implementation of {@link RequestResponseCacheFactory}.
 *
 * @author Felix Hellman
 */
public class RedisRequestResponseCacheFactory implements RequestResponseCacheFactory {

  private final RedisTemplate<String, RequestResponseEntry> requestResponseEntryRedisTemplate;
  private final RedisTemplate<String, String> requestSetTemplate;
  private final OpenIdFederationConfigurationProperties properties;

  /**
   * Constructor.
   * @param requestResponseEntryRedisTemplate
   * @param requestSetTemplate
   * @param properties
   */
  public RedisRequestResponseCacheFactory(
      final RedisTemplate<String, RequestResponseEntry> requestResponseEntryRedisTemplate,
      final RedisTemplate<String, String> requestSetTemplate,
      final OpenIdFederationConfigurationProperties properties) {

    this.requestResponseEntryRedisTemplate = requestResponseEntryRedisTemplate;
    this.requestSetTemplate = requestSetTemplate;
    this.properties = properties;
  }

  @Override
  public RequestResponseModuleCache create(final EntityID entityID) {
    return new RedisModuleRequestResponseCache(
        this.requestResponseEntryRedisTemplate,
        this.requestSetTemplate,
        entityID,
        this.properties.getCache().getRequestThreshold()
    );
  }
}
