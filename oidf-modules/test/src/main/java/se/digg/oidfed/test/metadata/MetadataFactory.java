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

package se.digg.oidfed.test.metadata;

import java.util.List;
import java.util.Map;

public class MetadataFactory {


  public Map<String, Object> relyingParty() {
    final Map<String, Object> metadata = Map.of(
        "application_type", "web",
        "redirect_uris", List.of("https://openid.swedenconnect.se"),
        "organization_name", "Sweden Connect",
        "grant_types", List.of("authorization_code", "client_credentials")
    );
    return Map.of("openid_relying_party", metadata);
  }

  public Map<String, Object> openIdProvider(final String issuer) {
    final Map<String, Object> metadata = Map.of(
        "issuer", issuer,
        "authorization_endpoint", "https://openid.swedenconnect.se/authorization",
        "grant_types_supported", List.of("authorization_code")
    );
    return Map.of("openid_provider", metadata);
  }

  //https://federation.se/sub1/sub2?fetch=entity

  public Map<String, Object> trustAnchor(final String entity) {
    final Map<String , Object> metadata = Map.of("federation_fetch_endpoint", "%s/fetch".formatted(entity),
        "federation_resolve_endpoint", "%s/resolve".formatted(entity),
        "federation_list_endpoint", "%s/list".formatted(entity),
        "organization_name", "Sweden Connect"
    );

    return Map.of("federation_entity", metadata);
  }


  public Map<String, Object> federationEntity() {
    return Map.of(
        "federation_entity",Map.of("organization_name", "Sweden Connect")
    );
  }
}
