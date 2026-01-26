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
package se.swedenconnect.oidf;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.Assert;

/**
 * Common properties for rest-clients
 *
 * @author Felix Hellman
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
   * Validate property.
   * @param key of parent
   */
  public void validate(final String key) {
    Assert.notNull(this.trustStoreBundleName, "%s.%s can not be empty".formatted(key,"trust-store-bundle-name"));
    Assert.notNull(this.name, "%s.%s can not be empty".formatted(key,"name"));
  }
}
