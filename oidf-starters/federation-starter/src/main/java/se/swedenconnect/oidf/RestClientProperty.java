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
package se.swedenconnect.oidf;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;

import java.time.Duration;

/**
 * Common properties for rest-clients
 *
 * @author Felix Hellman
 * @author Martin Lindström
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestClientProperty {
  private String baseUri;
  private String trustStoreBundleName;
  private String name;

  /**
   * Connect and read timeouts. If not set, or if a setting is missing, the default for the client applies.
   */
  @NestedConfigurationProperty
  private @Nullable Timeout timeout;

  /**
   * Validate property.
   * @param key of parent
   */
  public void validate(final String key) {
    Assert.notNull(this.trustStoreBundleName, "%s.%s can not be empty".formatted(key,"trust-store-bundle-name"));
    Assert.notNull(this.name, "%s.%s can not be empty".formatted(key,"name"));
  }

  /**
   * Connect and read timeouts for a rest client.
   *
   * @author Martin Lindström
   */
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Timeout {

    /**
     * Maximum time to establish the connection.
     */
    private @Nullable Duration connect;

    /**
     * Maximum time to wait for the response.
     */
    private @Nullable Duration read;
  }
}
