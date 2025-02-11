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
package se.digg.oidfed.test.testcontainer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.MountableFile;

  public class RelyingPartyContainer extends GenericContainer<RelyingPartyContainer> {

  public RelyingPartyContainer() {
    super("ghcr.io/swedenconnect/openid-federation-relying-party:latest");
    this.withEnv("SPRING_CONFIG_LOCATION", "/data/application.yml");
    this.withCopyFileToContainer(
        MountableFile.forClasspathResource("signkey.p12"),
        "/data/key.p12"
    );
    this.withCopyFileToContainer(
        MountableFile.forClasspathResource("rp-application.yml"), "/data/application.yml");
    this.withNetworkAliases("relyingparty");

    this.setWaitStrategy(new AbstractWaitStrategy() {
      @Override
      protected void waitUntilReady() {

      }
    });
  }
}
