package se.digg.oidfed.service.resolver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.resolver.ResolverRequest;

import java.util.Map;

public class ResolverClient {
  private final RestClient client = RestClient.builder().build();

  private final int port;

  public ResolverClient(final int port) {
    this.port = port;
  }

  public String resolve(final ResolverRequest request) {
    final ResponseEntity<String> entity =
        client.get().uri("http://localhost:%d/resolver/resolve?sub={sub}&anchor={anchor}".formatted(port), Map.of(
                "sub", request.subject(),
                "anchor", request.trustAnchor())
            )
            .retrieve()
            .toEntity(String.class);

    return entity.getBody();
  }
}
