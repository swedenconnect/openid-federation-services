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
package se.digg.oidfed.suites;

import com.redis.testcontainers.RedisContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.utility.DockerImageName;
import se.digg.oidfed.service.Application;
import se.digg.oidfed.service.GeneralErrorHandlingTestCases;
import se.digg.oidfed.service.actuator.ActuatorTestCases;
import se.digg.oidfed.service.entity.ApplicationReadyEndpoint;
import se.digg.oidfed.service.entity.EntityRegistryMockTestCases;
import se.digg.oidfed.service.entity.RegistryMock;
import se.digg.oidfed.service.resolver.ResolverTestCases;
import se.digg.oidfed.service.trustanchor.TrustAnchorTestCases;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkTestCases;
import se.digg.oidfed.test.testcontainer.RelyingPartyContainer;

import java.util.Random;

@Slf4j
@Suite
@SuiteDisplayName("Redis Test Suite")
@SelectClasses(value = {
    GeneralErrorHandlingTestCases.class,
    TrustMarkTestCases.class,
    TrustAnchorTestCases.class,
    ActuatorTestCases.class,
    EntityRegistryMockTestCases.class,
    ResolverTestCases.class
})
public class RedisTestSuite {

  private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:6.2.6"));

  private static final NginxContainer nginx = new NginxContainer(DockerImageName.parse("nginx:stable"));

  private static final RelyingPartyContainer relyingParty = new RelyingPartyContainer();

  private static ConfigurableApplicationContext configurableApplicationContext;

  private static RegistryMock registryMock;

  @BeforeSuite
  public static void start() throws InterruptedException {
    // Configure default environment
    EnvironmentConfigurators.configureDefaultEnvironment(nginx, relyingParty, log);
    // Add redis configuration
    EnvironmentConfigurators.configureRedis(redis, log);
    relyingParty.start();
    nginx.start();
    redis.start();
    try {
      final int port = new Random().nextInt(10000 - 9000) + 9000;
      registryMock = new RegistryMock(port);
      registryMock.init("4860ae57-9716-492b-951c-2a8c334f790a");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    configurableApplicationContext = new SpringApplicationBuilder()
        .sources(Application.class)
        .environment(new MockEnvironment()
            .withProperty("server.port", "6000")
            .withProperty("management.server.port", "6001")
            .withProperty("spring.data.redis.url", redis.getRedisURI())
            .withProperty("openid.federation.registry.integration.endpoints.base-path",
                "http://localhost:%d/api/v1".formatted(registryMock.getPort()) +
                    "/federationservice")
        )
        .profiles("integration-test")
        .run();
    ThreadLocal<ApplicationContext> threadLocalValue = new ThreadLocal<>();
    threadLocalValue.set(configurableApplicationContext);
    Context.applicationContext = threadLocalValue;
    GeneralErrorHandlingTestCases.serverPort = 6000;
    TrustMarkTestCases.serverPort = 6000;
    ActuatorTestCases.managementPort = 6001;
    ApplicationReadyEndpoint applicationReadyEndpoint =
        configurableApplicationContext.getBean(ApplicationReadyEndpoint.class);
    while (!applicationReadyEndpoint.applicationReady()) {
      log.info("Application not ready yet.. waiting for setup");
      Thread.sleep(500L);
    }
  }

  @AfterSuite
  public static void stop() {
    configurableApplicationContext.stop();
    relyingParty.stop();
    nginx.stop();
    redis.stop();
    registryMock.stop();
  }
}
