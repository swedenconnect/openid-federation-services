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
package se.swedenconnect.oidf;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;

/**
 * Tests for the timeouts of {@link RestClientFactory}.
 */
class RestClientFactoryTest {

  private final RestClientFactory factory =
      new RestClientFactory(new DefaultSslBundleRegistry(), ObservationRegistry.NOOP);

  @Test
  void readTimeoutFromPropertyIsApplied() throws Exception {
    final RestClientProperty property = this.property(new RestClientProperty.Timeout(null, Duration.ofMillis(200)));
    final RestClient client = this.factory.create(property,
        new RestClientProperty.Timeout(Duration.ofSeconds(5), Duration.ofSeconds(30)));

    this.assertTimesOut(client, Duration.ofSeconds(5));
  }

  @Test
  void defaultReadTimeoutIsAppliedWhenNotSet() throws Exception {
    final RestClient client = this.factory.create(this.property(null),
        new RestClientProperty.Timeout(Duration.ofSeconds(5), Duration.ofMillis(200)));

    this.assertTimesOut(client, Duration.ofSeconds(5));
  }

  private RestClientProperty property(final RestClientProperty.Timeout timeout) {
    final RestClientProperty property = new RestClientProperty();
    property.setName("test");
    property.setTrustStoreBundleName("default");
    property.setTimeout(timeout);
    return property;
  }

  /**
   * Sends a request to a server that accepts the connection but never answers, and asserts that the request fails
   * before {@code limit}.
   */
  private void assertTimesOut(final RestClient client, final Duration limit) throws Exception {
    try (final ServerSocket server = new ServerSocket(0)) {
      final Thread acceptor = Thread.ofVirtual().start(() -> {
        try (final Socket socket = server.accept()) {
          Thread.sleep(limit.toMillis() * 2);
        }
        catch (final Exception e) {
          // ignore
        }
      });
      final long start = System.nanoTime();
      Assertions.assertThrows(ResourceAccessException.class,
          () -> client.get().uri("http://localhost:" + server.getLocalPort() + "/").retrieve().toBodilessEntity());
      Assertions.assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(limit) < 0);
      acceptor.interrupt();
    }
  }
}
