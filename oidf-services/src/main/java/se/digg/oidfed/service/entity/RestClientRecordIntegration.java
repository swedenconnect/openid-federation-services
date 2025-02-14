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
package se.digg.oidfed.service.entity;

import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;
import se.digg.oidfed.common.entity.integration.Expirable;
import se.digg.oidfed.common.entity.integration.registry.ModuleResponse;
import se.digg.oidfed.common.entity.integration.registry.RegistryVerifier;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkSubjectRecord;
import se.digg.oidfed.common.entity.integration.registry.RecordRegistryIntegration;

import java.util.List;
import java.util.UUID;

/**
 * {@link RestClient} implementation for {@link RecordRegistryIntegration}
 *
 * @author Felix Hellman
 */
public class RestClientRecordIntegration implements RecordRegistryIntegration {

  private final RegistryVerifier verifier;
  private final RestClient client;

  /**
   * @param verifier for verifying records
   * @param client for fetching records
   */
  public RestClientRecordIntegration(final RegistryVerifier verifier, final RestClient client) {
    this.verifier = verifier;
    this.client = client;
  }

  @Override
  public Expirable<PolicyRecord> getPolicy(final String id) {
    final String body = this.client.get()
        .uri(builder -> builder
            .path("/policy_record")
            .queryParam("policy_id", id)
            .build())
        .retrieve()
        .body(String.class);
    return this.verifier.verifyPolicyRecord(body);
  }

  @Override
  public Expirable<List<EntityRecord>> getEntityRecords(final String issuer) {
    final String body = this.client.get()
        .uri(builder -> builder
            .queryParam("iss", issuer)
            .path("/entity_record")
            .build())
        .retrieve()
        .body(String.class);
    return this.verifier.verifyEntityRecords(body);
  }

  @Override
  public Expirable<ModuleResponse> getModules(final UUID instanceId) {
    final String body = this.client.get()
        .uri(builder -> builder
            .path("/submodules")
            .queryParam("instanceid", instanceId.toString())
            .build())
        .retrieve()
        .body(String.class);
    return this.verifier.verifyModuleResponse(body);
  }

  @Override
  public Expirable<List<TrustMarkSubjectRecord>> getTrustMarkSubject(final String issuer, final String trustMarkId) {
    final String body = this.client.get()
        .uri(builder -> builder
            .queryParam("iss", issuer)
            .queryParam("trustmark_id", trustMarkId)
            .path("/trustmarksubject_record")
            .build())
        .retrieve()
        .body(String.class);
    return this.verifier.verifyTrustMarkSubjects(body);
  }
}
