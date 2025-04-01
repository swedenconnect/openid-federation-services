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
package se.swedenconnect.oidf.service.resolver;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.google.common.collect.MapDifference;
import org.testcontainers.shaded.com.google.common.collect.Maps;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ResolverDifferentiator {
  private final RestClient client;
  private final EntityID anarchyTrustAnchor;
  private final EntityID anarchyResolver;
  private final EntityID trustAnchor;
  private final EntityID resolver;

  public ResolverDifferentiator(
      final RestClient client,
      final EntityID anarchyTrustAnchor,
      final EntityID anarchyResolver,
      final EntityID trustAnchor,
      final EntityID resolver) {

    this.client = client;
    this.anarchyTrustAnchor = anarchyTrustAnchor;
    this.anarchyResolver = anarchyResolver;
    this.trustAnchor = trustAnchor;
    this.resolver = resolver;
  }

  public ResponseDifference getResponseDifference(final ResolveRequest resolveRequest) {
    final Response response = this.getResponse(resolveRequest, this.resolver.getValue());

    final ResolveRequest referenceRequest = new ResolveRequest(
        resolveRequest.subject(),
        this.anarchyTrustAnchor.getValue(),
        resolveRequest.type()
    );

    final Response reference = this.getResponse(referenceRequest, this.anarchyResolver.getValue());

    return new ResponseDifference(reference, response);
  }


  private Response getResponse(final ResolveRequest resolveRequest, final String resolver) {
    final Response response = new Response();
    final String responseBody = this.client.mutate()
        .baseUrl(resolver)
        .build()
        .get()
        .uri(uri -> uri.path("/resolve")
            .queryParam("sub", resolveRequest.subject())
            .queryParam("trust_anchor", resolveRequest.trustAnchor())
            .queryParamIfPresent("entity_type", Optional.ofNullable(resolveRequest.type()))
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
          final String body = new String(res.getBody().readAllBytes());
          response.setError(Response.Error.builder()
              .statusCode(res.getStatusCode().value())
              .message(body)
              .build());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
          final String body = new String(res.getBody().readAllBytes());
          response.setError(Response.Error.builder()
              .statusCode(res.getStatusCode().value())
              .message(body)
              .build());
        })
        .body(String.class);

    response.setBody(responseBody);

    return response;
  }


  @Getter
  @Setter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @ToString
  public static class ResponseDifference {
    private Response reference;
    private Response response;

    private static final List<String> IGNORED_FIELDS = List.of("iat", "exp", "trust_chain", "iss");

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, MapDifference.ValueDifference<Object>> getJsonDifference() throws ParseException {

      final Map<String, MapDifference.ValueDifference<Object>> stringValueDifferenceMap = Maps.difference(
          SignedJWT.parse(this.reference.body).getJWTClaimsSet().toJSONObject(),
          SignedJWT.parse(this.response.body).getJWTClaimsSet().toJSONObject()
      ).entriesDiffering();

      final Map<String, MapDifference.ValueDifference<Object>> result = new HashMap<>(stringValueDifferenceMap);
      IGNORED_FIELDS.forEach(result::remove);
      return result;
    }
    public Map<String, MapDifference.ValueDifference<Object>> getTrustChainEntryDifference(final int index) throws ParseException {
      final SignedJWT responseJwt = SignedJWT.parse(this.response.body);
      final SignedJWT referenceJwt = SignedJWT.parse(this.reference.body);

      final String responseEntry = responseJwt.getJWTClaimsSet().getStringListClaim("trust_chain").get(index);
      final String referenceEntry = referenceJwt.getJWTClaimsSet().getStringListClaim("trust_chain").get(index);

      final Map<String, MapDifference.ValueDifference<Object>> stringValueDifferenceMap = Maps.difference(
          SignedJWT.parse(responseEntry).getJWTClaimsSet().toJSONObject(),
          SignedJWT.parse(referenceEntry).getJWTClaimsSet().toJSONObject()
      ).entriesDiffering();

      final Map<String, MapDifference.ValueDifference<Object>> result = new HashMap<>(stringValueDifferenceMap);


      final Map<String, Object> left = Maps.difference(
          SignedJWT.parse(responseEntry).getJWTClaimsSet().toJSONObject(),
          SignedJWT.parse(referenceEntry).getJWTClaimsSet().toJSONObject()
      ).entriesOnlyOnLeft();

      left.entrySet().forEach(kv -> {
        result.put(kv.getKey(), new MapDifference.ValueDifference<Object>() {
          @Override
          public Object leftValue() {
            return kv.getValue();
          }

          @Override
          public Object rightValue() {
            return null;
          }
        });
      });

      IGNORED_FIELDS.forEach(result::remove);
      return result;
    }
  }

  @Getter
  @Setter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @ToString
  public static class Response {
    private Error error;
    private String body;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Error {
      private int statusCode;
      private String message;
    }
  }

}
