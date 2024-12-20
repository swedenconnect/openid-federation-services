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

import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory class for creating paths.
 *
 * @author Felix Hellman
 */
public class EntityPathFactory {

  private final Pattern pattern = Pattern.compile("^(?<basePath>https://[a-z.-]*)(?<path>/.*)?$");

  private final List<String> allowedBasePaths;

  /**
   * @param allowedBasePaths for this instance
   */
  public EntityPathFactory(final List<String> allowedBasePaths) {
    this.allowedBasePaths = allowedBasePaths.stream().map(p -> {
      final Matcher matcher = this.pattern.matcher(p);
      if (matcher.matches()) {
        return matcher.group("basePath");
      }
      return p;
    }).toList();
  }

  /**
   * @param record   to calculate path for
   * @return relative path of the basepath
   */
  public String getPath(final EntityRecord record) {
    final Optional<String> hostedLocation = Optional.ofNullable(record.getOverrideConfigurationLocation())
        .map(location -> {
          if (location.startsWith("/")) {
            return location;
          }
          return this.allowedBasePaths.stream()
              .filter(location::startsWith)
              .map(issuer -> location.replace(issuer, ""))
              .findFirst()
              .orElseThrow(() -> {
                final String errorMessage = "Override Configuration Location is not allowed";
                return new IllegalArgumentException(errorMessage);
              });
        });
    if (hostedLocation.isPresent()) {
      return hostedLocation.get();
    }
    final Optional<String> allowedIssuer = this.allowedBasePaths.stream()
        .filter(basepath -> record.getSubject().getValue().contains(basepath))
        .findFirst();
    if (allowedIssuer.isEmpty()) {
      throw new IllegalArgumentException("Failed to determine path for hosted record");
    }
    final String key = record
        .getSubject()
        .getValue()
        .replace(allowedIssuer.get(), "");
    if (key.isEmpty() || key.isBlank()) {
      return "/";
    }
    return key;
  }
}
