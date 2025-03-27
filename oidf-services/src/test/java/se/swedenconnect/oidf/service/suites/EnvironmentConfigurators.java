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
import org.slf4j.Logger;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import se.swedenconnect.oidf.test.testcontainer.RelyingPartyContainer;

import java.util.List;

public class EnvironmentConfigurators {
  public static void configureRedis(final RedisContainer redis, final Logger log) {
    redis.withLogConsumer(new Slf4jLogConsumer(log));
  }

  public static void configureTestContainers() {
    Testcontainers.exposeHostPorts(6000);
    Testcontainers.exposeHostPorts(11000);
  }

  public static void configureRelyingParty(final RelyingPartyContainer relyingParty, final Logger log) {
    relyingParty.withLogConsumer(new Slf4jLogConsumer(log));
    relyingParty.withExposedPorts(11000);
    relyingParty.setPortBindings(List.of("11000:11000"));
    relyingParty.setWaitStrategy(new HostPortWaitStrategy().forPorts(11000));
  }

  /**
   * Configures Testcontainer openings and relying party container
   */
  public static void configureDefaultEnvironment(
      final RelyingPartyContainer relyingPartyContainer,
      final Logger log
  ) {
    configureRelyingParty(relyingPartyContainer, log);
    configureTestContainers();
  }
}
