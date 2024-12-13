/*
 * Copyright 2024 Sweden Connect
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
package se.digg.oidfed.common.entity;

import java.util.Optional;

/**
 * Factory class for creating paths.
 *
 * @author Felix Hellman
 */
public class EntityPathFactory {
  /**
   * @param record to calculate path for
   * @param basePath to apply
   * @return relative path of the basepath
   */
  public static String getPath(final EntityRecord record, final String basePath) {
    final Optional<String> hostedLocation = Optional.ofNullable(record.getOverrideConfigurationLocation())
        .map(location -> {
      if (location.startsWith("/")) {
        return location;
      }
      if (location.startsWith(basePath)) {
        return location.replace(basePath, "");
      }
      throw new IllegalArgumentException("Override Configuration Location is not relative or leads to another domain");
    });
    if (hostedLocation.isPresent()) {
      return hostedLocation.get();
    }
    if (!record.getSubject().getValue().contains(basePath)) {
      throw new IllegalArgumentException("Failed to determine path for hosted record");
    }
    final String key = record
        .getSubject()
        .getValue()
        .replace(basePath, "");
    if (key.isEmpty() || key.isBlank()) {
      return "/";
    }
    return key;
  }
}
