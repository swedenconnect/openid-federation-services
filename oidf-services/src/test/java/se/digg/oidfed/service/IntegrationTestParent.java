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
package se.digg.oidfed.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.redis.testcontainers.RedisContainer;
import io.netty.util.internal.SocketUtils;
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
import org.springframework.test.util.TestSocketUtils;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.entity.ApplicationReadyEndpoint;
import se.digg.oidfed.service.entity.EntityInitializer;
import se.digg.oidfed.service.entity.RegistryMock;
import se.digg.oidfed.service.modules.ModuleSetupCompleteEvent;
import se.digg.oidfed.service.testclient.TestFederationClientParameterResolver;
import se.digg.oidfed.test.testcontainer.RelyingPartyContainer;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=6000"})
@ActiveProfiles("integration-test")
@ExtendWith(TestFederationClientParameterResolver.class)
@Slf4j
public class IntegrationTestParent {

  public IntegrationTestParent() {
  }

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

  private static final RelyingPartyContainer relyingParty = new RelyingPartyContainer();

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
    relyingParty.withLogConsumer(new Slf4jLogConsumer(log));
    Testcontainers.exposeHostPorts(6000);
    Testcontainers.exposeHostPorts(11000);
    nginx.withExposedPorts(443);
    relyingParty.withExposedPorts(11000);
    nginx.withAccessToHost(true);
    nginx.setPortBindings(List.of("443:443"));
    relyingParty.setPortBindings(List.of("11000:11000"));
    relyingParty.start();
    nginx.start();
    redis.start();
  }

  @DynamicPropertySource
  static void registerRedisProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.url", redis::getRedisURI);
    registry.add("openid.federation.registry.integration.endpoints.base-path", () -> "http://localhost:9090/api/v1/federationservice");
  }


  static {
    try {
      new RegistryMock().init ("4860ae57-9716-492b-951c-2a8c334f790a");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  void integrationTestBefore() throws JOSEException, InterruptedException, IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
    while (!applicationReadyEndpoint.applicationReady()) {
      log.info("Application not ready yet.. waiting for setup");
      Thread.sleep(500L);
    }

    publisher.publishEvent(new ModuleSetupCompleteEvent());
  }
}
