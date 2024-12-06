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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Body;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.EntityRecordSigner;
import se.digg.oidfed.common.entity.HostedRecord;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.IntegrationTestParent;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

@ActiveProfiles("integration-test")
public class EntityRegistryIT extends IntegrationTestParent {

  @Autowired
  KeyRegistry registry;

  @Autowired
  EntityInitializer initializer;

  @Test
  void test() {
    final RestClient client = RestClient.builder().build();

    final ResponseEntity<String> root =
        client.get()
            .uri("http://localhost:%d/.well-known/openid-federation".formatted(serverPort))
            .retrieve().toEntity(String.class);

    Assertions.assertNotNull(root);
    System.out.println(root);

    final ResponseEntity<String> entityRoot =
        client.get()
            .uri("http://localhost:%d/root/.well-known/openid-federation".formatted(serverPort))
            .retrieve().toEntity(String.class);

    Assertions.assertNotNull(entityRoot);
    System.out.println(entityRoot);

    final ResponseEntity<String> entitySecond =
        client.get()
            .uri("http://localhost:%d/root/second/.well-known/openid-federation".formatted(serverPort))
            .retrieve().toEntity(String.class);

    Assertions.assertNotNull(entitySecond);
    System.out.println(entitySecond);
  }

  @Test
  void dynamicRegistration() throws JOSEException, ParseException {
    final JWKSet set = registry.getSet(List.of("sign-key-1"));
    final EntityRecordSigner entityRecordSigner = new EntityRecordSigner(new RSASSASigner(set.getKeys().getFirst().toRSAKey()));

    final String body = entityRecordSigner.signRecords(List.of(
        EntityRecord.builder()
            .issuer(new EntityID("http://localhost.test:9090/iss"))
            .subject(new EntityID("http://localhost.test:9090/sub"))
            .policyRecordId("policy")
            .hostedRecord(HostedRecord.builder().metadata(Map.of("federation_entity", Map.of("organization_name",
                "orgName"))).build())
            .build()
    )).serialize();

    WireMock.stubFor(WireMock.get("/registry/v1/entities").willReturn(
        new ResponseDefinitionBuilder().withResponseBody(new Body(body)))
    );


    initializer.handle(null);

    final RestClient client = RestClient.builder().build();

    final ResponseEntity<String> entitySecond =
        client.get()
            .uri("http://localhost:%d/sub/.well-known/openid-federation".formatted(serverPort))
            .retrieve().toEntity(String.class);

    System.out.println(entitySecond.getBody());
  }
}
