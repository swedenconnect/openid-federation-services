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
package se.swedenconnect.oidf.service.suites;

import com.redis.testcontainers.RedisContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.utility.DockerImageName;
import se.swedenconnect.oidf.service.Application;
import se.swedenconnect.oidf.service.entity.ApplicationReadyEndpoint;
import se.swedenconnect.oidf.service.entity.RegistryMock;
import se.swedenconnect.oidf.service.resolver.ResolverConstraintTestCases;
import se.swedenconnect.oidf.service.resolver.ResolverCritTestCases;
import se.swedenconnect.oidf.service.resolver.ResolverPolicyTestCases;
import se.swedenconnect.oidf.service.resolver.ResolverTrustMarkTestCases;
import se.swedenconnect.oidf.service.service.GeneralErrorHandlingTestCases;
import se.swedenconnect.oidf.service.service.actuator.ActuatorTestCases;
import se.swedenconnect.oidf.service.trustanchor.TrustAnchorTestCases;
import se.swedenconnect.oidf.service.trustmarkissuer.TrustMarkTestCases;

import java.util.Random;

@Slf4j
@Suite
@SuiteDisplayName("Redis Test Suite")
@SelectClasses(value = {
    ResolverConstraintTestCases.class,
    ResolverTrustMarkTestCases.class,
    GeneralErrorHandlingTestCases.class,
    TrustMarkTestCases.class,
    TrustAnchorTestCases.class,
    ActuatorTestCases.class,
    ResolverPolicyTestCases.class,
    ResolverCritTestCases.class
})
public class RedisTestSuite {

  private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis/redis-stack:latest"));


  private static ConfigurableApplicationContext configurableApplicationContext;

  private static RegistryMock registryMock;

  @BeforeSuite
  public static void start() throws InterruptedException {
    // Add redis configuration
    EnvironmentConfigurators.configureRedis(redis, log);
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
        .profiles("entitytypes")
        .environment(new MockEnvironment()
            .withProperty("server.port", "11111")
            .withProperty("management.server.port", "6001")
            .withProperty("spring.data.redis.url", redis.getRedisURI())
            .withProperty("federation.registry.integration.endpoints.base-path",
                "http://localhost:%d/api/v1".formatted(registryMock.getPort()) +
                    "/federationservice")
        )
        .profiles("integration-test")
        .run();
    ThreadLocal<ApplicationContext> threadLocalValue = new ThreadLocal<>();
    threadLocalValue.set(configurableApplicationContext);
    Context.applicationContext = threadLocalValue;
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
    redis.stop();
    registryMock.stop();
  }
}
