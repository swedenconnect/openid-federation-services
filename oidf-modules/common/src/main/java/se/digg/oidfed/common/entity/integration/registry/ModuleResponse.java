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
package se.digg.oidfed.common.entity.integration.registry;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Data response from registry to configure runtime modules from.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class ModuleResponse implements Serializable {
  private List<ResolverModuleResponse> resolvers;
  private List<TrustAnchorModuleResponse> trustAnchors;
  private List<TrustMarkIssuerModuleResponse> trustMarkIssuers;

  /**
   * Creates instance from json object {@link java.util.HashMap}
   *
   * @param json to read
   * @return new instance
   */
  public static ModuleResponse fromJson(final Map<String, Object> json) {
    final ModuleResponse response = new ModuleResponse();
    response.resolvers = Optional.ofNullable(json.get("resolvers")).map(resolvers -> {
          return ((List<Map<String, Object>>) resolvers)
              .stream()
              .map(ResolverModuleResponse::fromJson)
              .toList();
        })
        .orElse(List.of());
    response.trustAnchors = Optional.ofNullable(json.get("trust-anchors")).map(ta -> {
      return ((List<Map<String, Object>>) ta)
          .stream()
          .map(TrustAnchorModuleResponse::fromJson)
          .toList();
    }).orElse(List.of());

    response.trustMarkIssuers = Optional.ofNullable(json.get("trust-mark-issuers")).map(tmi -> {
      return ((List<Map<String, Object>>) tmi)
          .stream()
          .map(TrustMarkIssuerModuleResponse::fromJson)
          .toList();
    }).orElse(List.of());
    return response;
  }

  /**
   * @return this instance as json
   */
  public Map<String, Object> toJson() {
    final HashMap<String, Object> json = new HashMap<>();
    Optional.ofNullable(this.resolvers).map(r -> json.put("resolvers",
        this.resolvers.stream().map(ResolverModuleResponse::toJson).toList()));
    Optional.ofNullable(this.trustAnchors).map(t -> json.put("trust-anchors",
        this.trustAnchors.stream().map(TrustAnchorModuleResponse::toJson).toList()));
    Optional.ofNullable(this.trustMarkIssuers).map(t -> json.put("trust-mark-issuers",
        this.trustMarkIssuers.stream().map(TrustMarkIssuerModuleResponse::toJson).toList()));
    return json;
  }

  /**
   * @return list of all issuers
   */
  public List<String> getIssuers() {
    return Stream.of(
            this.resolvers.stream().map(ResolverModuleResponse::getEntityIdentifier),
            this.trustAnchors.stream().map(TrustAnchorModuleResponse::getEntityIdentifier),
            this.trustMarkIssuers.stream().map(TrustMarkIssuerModuleResponse::getEntityIdentifier)
        )
        .reduce(Stream::concat)
        .orElseGet(Stream::empty)
        .toList();
  }
}
