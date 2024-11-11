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
package se.digg.oidfed.service.resolver;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;
import se.digg.oidfed.resolver.ResolverRequest;
import se.digg.oidfed.test.FederationEntity;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class ResolverIT {

  @LocalServerPort
  int serverPort;

  private static List<FederationEntity> entityList;

  private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:6.2.6"));


  static {
    redis.start();
  }

  @DynamicPropertySource
  static void registerRedisProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.url", redis::getRedisURI);
  }


  static {
    final WireMockServer wireMockServer = new WireMockServer(9090);
    wireMockServer.start();
    WireMock.configureFor(9090);
    try {
      entityList = WiremockFederation.configure(9090);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to configure wiremock federation", e);
    }
  }

  @DynamicPropertySource
  static void dynamicProp(final DynamicPropertyRegistry registry) {
    final List<String> keysToTrust = entityList.stream()
        .map(entity -> entity.getSignKey().toPublicJWK())
        .map(key -> Base64.getEncoder().encode(key.toJSONString().getBytes(StandardCharsets.UTF_8)))
        .map(bytes -> new String(bytes, Charset.defaultCharset()))
        .distinct()
        .toList();
    for (int i = 0; i < keysToTrust.size(); i++) {
      final int index = i;
      registry.add("openid.federation.resolver.trusted-jwks[%d]".formatted(i), () -> keysToTrust.get(index));
    }
  }

  @Test
  void resolveFederation() {

    final ResolverClient resolverClient = new ResolverClient(serverPort);
    final String resolved = resolverClient.resolve(new ResolverRequest("",
        "http://localhost:9090/intermediate/relyingparty",
        "http://localhost:9090/trustanchor", "")
    );
  }
}