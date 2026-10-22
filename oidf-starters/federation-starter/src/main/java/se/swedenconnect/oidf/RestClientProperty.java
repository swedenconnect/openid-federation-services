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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;

import java.time.Duration;

/**
 * Common properties for rest-clients
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestClientProperty {
  private String baseUri;
  private String trustStoreBundleName;
  private String name;

  /**
   * Connection pool of this client. Each client gets its own pool, so that one workload cannot exhaust the
   * connections another one needs.
   */
  @NestedConfigurationProperty
  private Pool pool = new Pool();

  /**
   * Timeouts of this client. Every stage of a request is bounded, see {@link Timeouts}.
   */
  @NestedConfigurationProperty
  private Timeouts timeouts = new Timeouts();

  /**
   * Times a failed request is retried. Each retry costs another full {@link Timeouts#worstCase()}, so the default is
   * no retries. Stale pooled connections are handled by {@link Pool#validateAfterInactivity} rather than by retrying.
   */
  private int maxRetries = 0;

  /**
   * Interval between retries, when {@link #maxRetries} is above zero.
   */
  private Duration retryInterval = Duration.ofMillis(500);

  /**
   * Whether redirects are followed. A redirect costs another full {@link Timeouts#worstCase()}, and no federation
   * endpoint needs one, so the default is off. This matches the behaviour of the JDK client used before.
   */
  private boolean followRedirects = false;

  /**
   * Settings for a client used while serving a request. The timeouts are tight, since a request thread waits for
   * them, and the pool is small, since a burst of inbound requests to one host should not be amplified into a burst
   * of outbound connections to it.
   *
   * @return settings for a client in the request path
   */
  public static RestClientProperty requestPath() {
    final RestClientProperty property = new RestClientProperty();
    property.name = "federation-request";
    property.pool.setMaxTotal(50);
    property.pool.setMaxPerRoute(10);
    property.timeouts.setConnectionRequest(Duration.ofSeconds(1));
    property.timeouts.setConnect(Duration.ofSeconds(2));
    property.timeouts.setTlsHandshake(Duration.ofSeconds(2));
    property.timeouts.setResponse(Duration.ofSeconds(3));
    property.timeouts.setSocket(Duration.ofSeconds(3));
    return property;
  }

  /**
   * Validate property.
   * @param key of parent
   */
  public void validate(final String key) {
    Assert.notNull(this.trustStoreBundleName, "%s.%s can not be empty".formatted(key,"trust-store-bundle-name"));
    Assert.notNull(this.name, "%s.%s can not be empty".formatted(key,"name"));
    this.validateClient(key);
  }

  /**
   * Validates the pool and timeout settings. Called for every client, also for the ones whose
   * {@link #validate(String)} is not.
   *
   * @param key of parent
   */
  public void validateClient(final String key) {
    Assert.notNull(this.pool, "%s.%s can not be null".formatted(key, "pool"));
    Assert.notNull(this.timeouts, "%s.%s can not be null".formatted(key, "timeouts"));
    Assert.isTrue(this.maxRetries >= 0, "%s.%s can not be negative".formatted(key, "max-retries"));
    this.pool.validate("%s.%s".formatted(key, "pool"));
    this.timeouts.validate("%s.%s".formatted(key, "timeouts"));
  }

  /**
   * Connection pool settings of a client.
   *
   * @author Per Fredrik Plars
   */
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Pool {

    /**
     * Connections this client may hold in total, across all hosts.
     */
    private int maxTotal = 100;

    /**
     * Connections this client may hold to one host. The value has to cover the number of entities that share a host,
     * or requests to that host are serialized in waves of this size.
     */
    private int maxPerRoute = 24;

    /**
     * How long a pooled connection may be reused before it is closed, regardless of activity. Bounds how long a
     * connection to a host whose address has changed is kept.
     */
    private Duration timeToLive = Duration.ofMinutes(5);

    /**
     * How long a pooled connection may be idle before it is checked for staleness when leased. Guards against using
     * a connection the peer has already closed.
     */
    private Duration validateAfterInactivity = Duration.ofSeconds(5);

    /**
     * Idle connections are closed after this long. Set to zero to keep idle connections until their time to live
     * runs out.
     */
    private Duration evictIdleAfter = Duration.ofSeconds(30);

    /**
     * Validates the pool settings.
     *
     * @param key of parent
     */
    public void validate(final String key) {
      Assert.isTrue(this.maxTotal > 0, "%s.%s must be positive".formatted(key, "max-total"));
      Assert.isTrue(this.maxPerRoute > 0, "%s.%s must be positive".formatted(key, "max-per-route"));
      Assert.isTrue(this.maxPerRoute <= this.maxTotal,
          "%s.%s can not be greater than %s.%s".formatted(key, "max-per-route", key, "max-total"));
      Assert.notNull(this.timeToLive, "%s.%s can not be null".formatted(key, "time-to-live"));
      Assert.notNull(this.validateAfterInactivity,
          "%s.%s can not be null".formatted(key, "validate-after-inactivity"));
      Assert.notNull(this.evictIdleAfter, "%s.%s can not be null".formatted(key, "evict-idle-after"));
    }
  }

  /**
   * Timeout settings of a client. The stages are bounded separately, since a request that hangs in one of them is
   * not bounded by the others.
   *
   * @author Per Fredrik Plars
   */
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Timeouts {

    /**
     * Waiting for a connection from the pool. This is what turns an exhausted pool into a fast failure instead of a
     * request that hangs for as long as the pool stays busy.
     */
    private Duration connectionRequest = Duration.ofSeconds(5);

    /**
     * Establishing the TCP connection. Without it, a host that drops the connection attempt is only bounded by the
     * operating system, which took over four minutes in practice. Name resolution happens before this and is not
     * covered by it.
     */
    private Duration connect = Duration.ofSeconds(3);

    /**
     * Completing the TLS handshake, after the TCP connection is up.
     */
    private Duration tlsHandshake = Duration.ofSeconds(3);

    /**
     * Waiting for the first byte of the response, after the request has been sent.
     */
    private Duration response = Duration.ofSeconds(5);

    /**
     * Waiting between reads on the socket. Bounds a response that arrives but then stops part way through.
     */
    private Duration socket = Duration.ofSeconds(5);

    /**
     * The longest a single request can take, as the sum of the stages. Apache HttpClient has no timeout for a whole
     * request, so a caller that makes several requests under a deadline of its own has to budget with this.
     *
     * @return the worst case of one request, without retries or redirects
     */
    public Duration worstCase() {
      return this.connectionRequest
          .plus(this.connect)
          .plus(this.tlsHandshake)
          .plus(this.response);
    }

    /**
     * Validates the timeout settings.
     *
     * @param key of parent
     */
    public void validate(final String key) {
      requirePositive(this.connectionRequest, key, "connection-request");
      requirePositive(this.connect, key, "connect");
      requirePositive(this.tlsHandshake, key, "tls-handshake");
      requirePositive(this.response, key, "response");
      requirePositive(this.socket, key, "socket");
    }

    private static void requirePositive(final Duration value, final String key, final String name) {
      Assert.notNull(value, "%s.%s can not be null".formatted(key, name));
      Assert.isTrue(!value.isNegative() && !value.isZero(), "%s.%s must be positive".formatted(key, name));
    }
  }
}
