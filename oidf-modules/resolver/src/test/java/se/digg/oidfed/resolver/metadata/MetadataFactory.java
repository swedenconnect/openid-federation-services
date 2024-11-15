package se.digg.oidfed.resolver.metadata;

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
