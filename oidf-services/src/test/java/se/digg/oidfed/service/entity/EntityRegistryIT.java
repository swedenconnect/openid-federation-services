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
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
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

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@ActiveProfiles("integration-test")
@Slf4j
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

    final ResponseEntity<String> entityRoot =
        client.get()
            .uri("http://localhost:%d/root/.well-known/openid-federation".formatted(serverPort))
            .retrieve().toEntity(String.class);

    Assertions.assertNotNull(entityRoot);

    final ResponseEntity<String> entitySecond =
        client.get()
            .uri("http://localhost:%d/root/second/.well-known/openid-federation".formatted(serverPort))
            .retrieve().toEntity(String.class);

    Assertions.assertNotNull(entitySecond);
  }

  @Test
  void dynamicRegistration() throws ParseException {
    final RestClient client = RestClient.builder().baseUrl("http://localhost:%d".formatted(serverPort)).build();

    final ResponseEntity<String> entitySecond =
        client.get()
            .uri("http://localhost:%d/customsub/.well-known/openid-federation".formatted(serverPort))
            .retrieve().toEntity(String.class);

    final List<String> body =
        (List<String>) client.get().uri("/ta/subordinate_listing").retrieve().toEntity(List.class).getBody();

    Assertions.assertTrue(body.contains("http://localhost.test:9090/subsub"), "Subordinate listing does not contain " +
        "expected value %s was %s".formatted("http://localhost.test:9090/subsub", body));

    final String jwt = client.get()
        .uri("/ta/fetch?sub=%s".formatted(URLEncoder.encode((String) "http://localhost.test:9090/subsub", Charset.defaultCharset())))
        .retrieve().body(String.class);
    final String subjectEntityConfigurationLocation = SignedJWT.parse(jwt).getJWTClaimsSet()
        .getStringClaim("subject_entity_configuration_location")
        .replace("8080", "" + serverPort);

    Assertions.assertEquals("http://localhost.test:9090/customsub/.well-known/openid-federation",
        subjectEntityConfigurationLocation);
  }
}
