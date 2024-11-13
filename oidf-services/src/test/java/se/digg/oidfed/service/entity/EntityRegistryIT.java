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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public class EntityRegistryIT {

  @LocalServerPort
  int serverPort;

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
}
