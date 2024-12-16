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
package se.digg.oidfed.service.trustmarkissuer;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.PolicyRecord;
import se.digg.oidfed.common.entity.integration.RecordRegistryIntegration;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuerSubject;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuerSubjectLoader;
import se.digg.oidfed.trustmarkissuer.TrustMarkSubjectRecordVerifier;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RecordRegistry integration using {@link RestClient}
 *
 * @author Felix Hellman
 */
public class TMIRestClientRecordIntegration implements TrustMarkIssuerSubjectLoader {
  private final RestClient client;
  private final TrustMarkSubjectRecordVerifier verifier;

  /**
   * Constructor.
   *
   * @param client   to use
   * @param verifier to use
   */
  public TMIRestClientRecordIntegration(final RestClient client, final TrustMarkSubjectRecordVerifier verifier) {
    this.client = client;
    this.verifier = verifier;
  }

  @Override
  public List<TrustMarkIssuerSubject> loadSubject(final String issuerEntityId, final TrustMarkId trustMarkId,
      final Optional<String> subject) {

    final Map<String,String> params = new HashMap<>(3);
    params.put("iss",issuerEntityId);
    params.put("sub",subject.orElse(""));
    params.put("trustmark_id",trustMarkId.getTrustMarkId());

    try {
      final ResponseEntity<String> jwt = this.client.get()
          .uri("/api/v1/federationservice/entity_record?iss={iss}&sub={sub}&trustmark_id={trustmark_id}", params)
          .retrieve()
          .toEntity(String.class);
      return this.verifier.verifyTrustMarkSubjects(jwt.getBody());
    }
    catch (final HttpClientErrorException.NotFound e) {
      return List.of();
    }
  }
}

