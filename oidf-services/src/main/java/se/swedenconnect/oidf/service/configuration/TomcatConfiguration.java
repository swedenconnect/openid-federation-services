/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf.service.configuration;

import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat configuration.
 *
 * <p>
 * Hosted entities may use a virtual entity id with a URL-encoded entity identifier, e.g.
 * {@code <ie>/hosted/https%3A%2F%2Frp.example.se}. Tomcat rejects encoded slashes by default, so they are passed
 * through to the routing as-is.
 * </p>
 *
 * @author Felix Hellman
 */
@Configuration
public class TomcatConfiguration {

  @Bean
  TomcatConnectorCustomizer encodedSolidusPassthroughCustomizer() {
    return connector -> connector.setEncodedSolidusHandling("passthrough");
  }
}
