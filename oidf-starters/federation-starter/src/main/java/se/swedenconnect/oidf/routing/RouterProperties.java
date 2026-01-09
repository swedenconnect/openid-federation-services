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

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.net.URI;
import java.util.List;

/**
 * Property class for router.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class RouterProperties {
  /**
   * Set to true to enable routing.
   * Disable this if you want to implement custom routing.
   */
  private Boolean enabled;
  /**
   * Mode to use when evaluating routes.
   */
  public enum DomainEvaluationMode {
    /**
     * Enforces host portion of route when routing traffic.
     * Required for mutli-domain mode to work.
     * It is not recommended to use context-path when using this mode.
     */
    STRICT,
    /**
     * Allows any host portion but does not validate host.
     */
    RELAXED,
    /**
     * Ignores host portion of route when routing traffic.
     */
    IGNORING
  }

  private DomainEvaluationMode mode;
  private List<URI> allowedDomains;

  /**
   * Validate properties.
   * @param key
   */
  public void validate(final String key) {
    if (this.enabled) {
      Assert.notNull(this.mode, "%s.%s can not be null, set to either of following %s"
          .formatted(key, "mode", DomainEvaluationMode.values()));
      if (DomainEvaluationMode.STRICT.equals(this.mode)) {
        Assert.notNull(this.allowedDomains, "%s.%s can not be null when domain evaluation mode is strict");
        Assert.notEmpty(this.allowedDomains, "%s.%s can not be empty when domain evaluation mode is strict");
      }
    }
  }
}
