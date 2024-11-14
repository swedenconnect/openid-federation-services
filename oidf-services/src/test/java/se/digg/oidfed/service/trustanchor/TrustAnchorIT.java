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
package se.digg.oidfed.service.trustanchor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.service.IntegrationTestParent;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class TrustAnchorIT extends IntegrationTestParent {



  @LocalServerPort
  int serverPort;

  @Test
  void test() {
    final RestClient client = RestClient.builder().baseUrl("http://localhost:%d".formatted(serverPort)).build();
    final List body = client.get().uri("/ta/subordinate_listing").retrieve().toEntity(List.class).getBody();
    System.out.println(body);
    Objects.requireNonNull(body);
    final String jwt = client.get()
        .uri("/ta/fetch?sub=%s".formatted(URLEncoder.encode((String) body.getFirst(), Charset.defaultCharset())))
        .retrieve().body(String.class);
    System.out.println(jwt);
  }
}