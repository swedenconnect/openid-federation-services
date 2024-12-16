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
package se.digg.oidfed.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Body;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.EntityRecordSigner;
import se.digg.oidfed.common.entity.HostedRecord;
import se.digg.oidfed.common.entity.PolicyRecord;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.entity.EntityInitializer;
import se.digg.oidfed.service.resolver.WiremockFederation;
import se.digg.oidfed.test.FederationEntity;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTestParent {
  @LocalServerPort
  public int serverPort;

  private static List<FederationEntity> entityList;

  @Autowired
  EntityInitializer entityInitializer;

  @Autowired
  KeyRegistry registry;

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

  @BeforeEach
  void before() throws JOSEException {
    final JWKSet set = registry.getSet(List.of("sign-key-1"));
    final EntityRecordSigner entityRecordSigner = new EntityRecordSigner(new RSASSASigner(set.getKeys().getFirst().toRSAKey()));

    final String body = entityRecordSigner.signRecords(List.of(
        EntityRecord.builder()
            .issuer(new EntityID("http://localhost.test:9090/root"))
            .subject(new EntityID("http://localhost.test:9090/sub"))
            .jwks(set)
            .policyRecordId("my-super-policy")
            .hostedRecord(HostedRecord.builder().metadata(Map.of("federation_entity", Map.of("organization_name",
                "orgName"))).build())
            .build()
    )).serialize();

    final String policyBody = entityRecordSigner.signPolicy(new PolicyRecord("my-super-policy", Map.of())).serialize();

    WireMock.stubFor(WireMock.get("/api/v1/federationservice/entity_record?iss=%s".formatted(URLEncoder.encode("http" +
        "://localhost.test:9090/root", Charset.defaultCharset()))).willReturn(
        new ResponseDefinitionBuilder().withResponseBody(new Body(body)))
    );

    WireMock.stubFor(WireMock.get("/api/v1/federationservice/policy_record?policy_id=%s".formatted(URLEncoder.encode(
        "my-super-policy", Charset.defaultCharset()))).willReturn(
        new ResponseDefinitionBuilder().withResponseBody(new Body(body)))
    );


    WireMock.stubFor(WireMock.get("/api/v1/federationservice/trustmarksubject_record?").willReturn(
        new ResponseDefinitionBuilder().withResponseBody(new Body(body)))
    );
    entityInitializer.handle(null);
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
      registry.add("openid.federation.key-registry.keys[%d].alias".formatted(i), () -> "key-%d".formatted(index));
      registry.add("openid.federation.key-registry.keys[%d].key".formatted(i), () -> keysToTrust.get(index));
      registry.add("openid.federation.resolver.resolvers[0].trusted-keys[%d]".formatted(i), () -> "key-%d".formatted(index));
    }
    registry.add("openid.federation.key-registry.keys[%d].alias".formatted(keysToTrust.size()), () -> "sign-key-1");
    registry.add("openid.federation.key-registry.keys[%d].key".formatted(keysToTrust.size()), () -> "eyJwIjoiOGVqdjlJYWFXVUZGVFNDcE1VVFd2cDA1Wl9zZW9pak5rZGFodWw3UWpqOGNVN1Y2LVd5YWl4NnlxYlZrUHR2eDNrRlFud2RSb2FBRGRseFZXcjRRY0MxWlBscjVDY2FGOXA2RkRtTDZGVi1SSThIVXNHTmp4UHBYTldPYUZSdHNWclFrdXh6NUpVelEzYm1SaGVjSmp0b0p5T1ZYRFZGcGFXbnZFZFZ2NFJjIiwia3R5IjoiUlNBIiwicSI6IjFYYzZpVEk0WUlTY0hRSFFjUWJzQndHbDJBOEhrckZ2YnRNU1kzV013aHV3WFpDdjhKTzA3ZWpkQUdUUlBtdFk0YTRtNkI4MEIxdEFYWE5feUdacGpkd1ZyTkNJbDAwUUh1VldjXzIyRzBFYy1MU2FqUmxFWmZoM1prMmd2cFdzMjZOeFdDLTdFdnRoTjgxcnc4d2J6eldPamdicjJ3S2pjZkM3ai05WGw5cyIsImQiOiJQY1UtM1FBWnR0Z2ZhSjYxZUNsN1hDRUpvTWRkUUI4dXdtSkE1TDdYbDRhZGlXNGs1elVZZUZlR0stZmRNUHdraGhYZExIcVEwbFZ2RmEyeVJlN09qVVo2bkFrZDRpX2VZZGY5YVRIVWNCM2hhRjZYNWxKU0x5WTVIVF9GM2ViQXlVcHBLc1lRQkRZMVJ4YS1GMTR5Nnk5TnhKelRESlFiQ1U1MldGazhET2JSdTctU25fTVZhbXNreUJJNng4eHdRY3B1QV9GOTJTMnowLTdxNHFlQmJIa3p3ZXRMQUNVUFhyWVNqbWh3aExiZ0lxNjB3UWlHdlY1QmNrWUFWX3VPV25VNWZONW1DbzBIYWlfTm1Ma19ROUpBZTdUSHRwRFI5N0NOM1ctR2Vza01tT2ladVAtVFRiZXlLQkdDRnF3TWJoZFdKX2Y1NlIyVEsxX0t3NUVRTFEiLCJlIjoiQVFBQiIsInVzZSI6InNpZyIsImtpZCI6IjVjMTMwZTliLWU2M2MtNDE4ZS1iNWU1LTA4ZWVjYzc5NTIyOSIsInFpIjoiTjdGZGdiRkNnbnZ2LXF4b3prckJDcXVWR2Y4a283RG1zbi1HdXgxMWpjNjVTX0RqYkd5V1FGVklOa3RlWkhQMUlyWVRaSUtZQmF6VmgzOVpZaHItMEFPQU5wQWVpZk1GRm5xQ3dLUzd5d3pUbTZBT2o2VkFQLWxtRXV4YURkNWJ1Zmw5WE16MUJsRnhQZjhaOGU5aDNYWTJ2NUJmNTBFZ2FDZkFHRU95MUY0IiwiZHAiOiJHMXZ3NTRMaHFmNkx5X0ZKc1Z6THZMc2padk1ydjhORm5KemRwYXBiZ19yM1JUQmRQQ0JnU2lPcXFTN3VxMzFNbVlwaGg5cllES09BUUw2b21KNnVWVUdMdWxXbm5NZHJGejFPWnhZaW1wQTRaZ2JoLXlFQ2c2Q2NoRmxEYi1ldUZSQkNwemJicHJCTlh3WkJ3eGNpS1puWFNYVkdweWJ4LVE0V0Q3cEg4UGsiLCJpYXQiOjE3MzA0NjczNTQsImRxIjoiU3NqcWlRWV9HaEZiWUE0eXFHWjBhal81aWlnNXp2cTZ2MmpUR0dVbVd4cTRQdzlobERjck8tNUlfc3BUUkRyM2VKazZxRGZHVW8xMWowZlhqSzNVYlE2ZHlWMkZmUHhTbVlCZk9XTzVXUE1HNDFyaWltd191am5DR3VVM1c0MjBjWDVoUnpQeHBrVExXbkZ0UDd0U1F0WFFpNUdCTDhsTTJhNHRCOUFpTC1FIiwibiI6InliZDZQbW9qazJTMk4wLTlKT3BMeEFBNV9JUjc3al9LaV85R3hTM0NrbVB4cWJCYkdGTmVlZmtFS2ZlNF9FZU9DbmNCQXhpc2hnU0lvWUExdVRmUVN5dmtVM2pDaTZaQmotZXpfVWs2T3Z6ZVpDSngwaFRNZU1kbnI4MDRTYnpQYWJsamY4UldhWjlhcGRxSnR4Q2Y3VUJrUXRMTkc5WG1ld01yLXZGNTNCamdFTlBlc3g0VWVYZHRkM3N3Tk9ycWJfT1g3dVZxaG9jYUs1VHppYml2b1FENEw2cXRVNWhYY0tuWDJmdURMN3lZVmM1am1LREd3X0V4RS1EMUNzamhNaHRUOHVnVmpRTHVOQy15TWFFZl9xVzR2RnA5MVNPbkR0bjBrQ3ZXYWF2dkJ6bmJqUUtfNVFzV1lRS3l6SGZtWmhQbHNSMFBHRTZ1Y0k0cGpFc2ZyUSJ9");
    registry.add("openid.federation.resolver.resolvers[0].alias", () -> "resolver");
    registry.add("openid.federation.resolver.resolvers[0].trust-anchor", () -> "http://localhost:9090/trustanchor");
    registry.add("openid.federation.resolver.resolvers[0].entity-identifier", () -> "http://localhost:9090/resolver");
    registry.add("openid.federation.resolver.resolvers[0].sign-key-alias", () -> "sign-key-1");
  }
}
