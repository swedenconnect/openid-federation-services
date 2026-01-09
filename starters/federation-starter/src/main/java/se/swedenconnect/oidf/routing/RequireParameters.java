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

import org.springframework.util.MultiValueMap;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;

import java.util.List;

/**
 * Parameter validation class for router routes.
 *
 * @author Felix Hellman
 */
public class RequireParameters {
  /**
   * @param params to check
   * @param requiredParameters to require
   * @return params if they are valid
   * @throws FederationException if invalid
   */
  public static MultiValueMap<String, String> validate(
      final MultiValueMap<String, String> params,
      final List<String> requiredParameters) throws FederationException {
    for (final String parameter : requiredParameters) {
      if (!params.containsKey(parameter)) {
        throw new InvalidRequestException("Required request parameter [%s] was missing.".formatted(parameter));
      }
    }
    return params;
  }
}