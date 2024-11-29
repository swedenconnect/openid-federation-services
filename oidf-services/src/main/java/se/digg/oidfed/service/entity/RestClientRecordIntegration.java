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
package se.digg.oidfed.service.entity;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.PolicyRecord;
import se.digg.oidfed.common.entity.RecordVerifier;
import se.digg.oidfed.common.entity.integration.RecordRegistryIntegration;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RecordRegistry integration using {@link RestClient}
 *
 * @author Felix Hellman
 */
public class RestClientRecordIntegration implements RecordRegistryIntegration {
  private final RestClient client;
  private final RecordVerifier verifier;

  private final ResponseErrorHandler errorHandler = new ResponseErrorHandler() {
    @Override
    public boolean hasError(final ClientHttpResponse response) throws IOException {
      return response.getStatusCode().value() == 404;
    }

    @Override
    public void handleError(final ClientHttpResponse response) throws IOException {
      throw new RuntimeException("Entity not found");
    }
  };

  /**
   * Constructor.
   *
   * @param client   to use
   * @param verifier to use
   */
  public RestClientRecordIntegration(final RestClient client, final RecordVerifier verifier) {
    this.client = client;
    this.verifier = verifier;
  }

  @Override
  public Optional<PolicyRecord> getPolicy(final String id) {
    final String uri = "/api/v1/federationservice/policy_record?policy_id={id}";
    final String jwt = client.get()
        .uri(uri, Map.of("id", URLEncoder.encode(id, Charset.defaultCharset())))
        .retrieve()
        .onStatus(errorHandler)
        .body(String.class);
    return verifier.verifyPolicy(jwt);
  }

  @Override
  public List<EntityRecord> getEntityRecords(final String issuer) {
    final String jwt = client.get().uri("/api/v1/federationservice/entity_record")
        .retrieve().onStatus(predicate -> predicate.value() == 404, (r, handler) -> {
          throw new RuntimeException("Not found");
        })
        .body(String.class);
    return verifier.verifyEntities(jwt);
  }
}

