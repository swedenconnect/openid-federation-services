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
package se.swedenconnect.oidf.service.router;

import lombok.Getter;
import lombok.Setter;

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
}
