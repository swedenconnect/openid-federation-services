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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.EntityRecordSigner;
import se.digg.oidfed.common.entity.HostedRecord;
import se.digg.oidfed.common.entity.PolicyRecord;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.entity.ApplicationReadyEndpoint;
import se.digg.oidfed.service.entity.EntityInitializer;
import se.digg.oidfed.service.entity.TestFederationEntities;
import se.digg.oidfed.service.modules.ModuleSetupCompleteEvent;
import se.digg.oidfed.service.testclient.TestFederationClientParameterResolver;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=6000"})
@ActiveProfiles("integration-test")
@ExtendWith(TestFederationClientParameterResolver.class)
@Slf4j
public class IntegrationTestParent {
  public static final EntityID RP_FROM_REGISTRY_ENTITY = new EntityID("https://municipality.local.swedenconnect.se/rp-from-registry");
  @LocalServerPort
  public int serverPort;

  @Autowired
  protected SslBundles bundles;

  @Autowired
  protected ApplicationReadyEndpoint applicationReadyEndpoint;

  @Autowired
  protected ApplicationEventPublisher publisher;

  @Autowired
  EntityInitializer entityInitializer;

  @Autowired
  KeyRegistry registry;

  private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:6.2.6"));

  private static final NginxContainer nginx = new NginxContainer(DockerImageName.parse("nginx:stable"));

  static {
    nginx.withNetworkAliases(
        "authorization.local.swedenconnect.se",
        "private.local.swedenconnect.se",
        "municipality.local.swedenconnect.se"
    );
    nginx.withCopyFileToContainer(MountableFile.forClasspathResource("/nginx/default.conf.template"), "/etc/nginx/templates/default.conf.template");
    nginx.withCopyFileToContainer(MountableFile.forClasspathResource("/nginx/mime.types"), "/etc/nginx/mime.types");
    nginx.withCopyFileToContainer(MountableFile.forClasspathResource("/nginx/ca.pem"), "/etc/nginx/ca.pem");
    nginx.withCopyFileToContainer(MountableFile.forClasspathResource("/nginx/server.crt"), "/etc/nginx/server.crt");
    nginx.withCopyFileToContainer(MountableFile.forClasspathResource("/nginx/server.key"), "/etc/nginx/server.key");
    nginx.withLogConsumer(new Slf4jLogConsumer(log));
    redis.withLogConsumer(new Slf4jLogConsumer(log));
    Testcontainers.exposeHostPorts(6000);
    nginx.withExposedPorts(443);
    nginx.withAccessToHost(true);
    nginx.setPortBindings(List.of("443:443"));
    nginx.start();
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
  }

  @BeforeEach
  public void integrationTestBefore() throws JOSEException, InterruptedException {
    while (!applicationReadyEndpoint.getResolverReady()) {
      log.info("Application not ready yet.. waiting for setup");
      Thread.sleep(500L);
    }


    final JWKSet set = registry.getSet(List.of("sign-key-1"));
    final EntityRecordSigner entityRecordSigner = new EntityRecordSigner(new RSASSASigner(set.getKeys().getFirst().toRSAKey()));

    final String body = entityRecordSigner.signRecords(List.of(
        EntityRecord.builder()
            .issuer(TestFederationEntities.Municipality.TRUST_ANCHOR)
            .subject(RP_FROM_REGISTRY_ENTITY)
            .policyRecordId("my-super-policy")
            .hostedRecord(HostedRecord.builder().metadata(Map.of("federation_entity", Map.of("organization_name",
                "Municipality"))).build())
            .build()
    )).serialize();

    final String emptyBody = entityRecordSigner.signRecords(List.of())
        .serialize();

    final String policyBody = entityRecordSigner.signPolicy(new PolicyRecord("my-super-policy", Map.of())).serialize();

    WireMock.stubFor(
        WireMock
            .get("/api/v1/federationservice/entity_record?iss=%s".formatted(URLEncoder.encode(TestFederationEntities.Municipality.TRUST_ANCHOR.getValue(), Charset.defaultCharset())))
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(body)))
    );

    WireMock.stubFor(
        WireMock
            .get("/api/v1/federationservice/entity_record?iss=%s".formatted(URLEncoder.encode(TestFederationEntities.Authorization.TRUST_ANCHOR.getValue(), Charset.defaultCharset())))
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(emptyBody)))
    );

    WireMock.stubFor(
        WireMock
            .get("/api/v1/federationservice/entity_record?iss=%s".formatted(URLEncoder.encode(TestFederationEntities.PrivateSector.TRUST_ANCHOR.getValue(), Charset.defaultCharset())))
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(emptyBody)))
    );

    WireMock
        .stubFor(WireMock.get("/api/v1/federationservice/policy_record?policy_id=my-super-policy")
            .willReturn(new ResponseDefinitionBuilder().withResponseBody(new Body(policyBody)))
        );


    WireMock.stubFor(WireMock.get("/api/v1/federationservice/trustmarksubject_record?").willReturn(
        new ResponseDefinitionBuilder().withResponseBody(new Body(body)))
    );
    entityInitializer.handleReload(null);
    publisher.publishEvent(new ModuleSetupCompleteEvent());
  }
}
