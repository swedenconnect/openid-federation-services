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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToDoubleFunction;

/**
 * Factory class for creating rest clients. Every client gets its own Apache HttpClient with its own connection pool,
 * so that the workload of one client cannot exhaust the connections another one needs, and every stage of a request
 * is bounded by a timeout.
 *
 * @author Felix Hellman
 */
@Slf4j
public class RestClientFactory implements DisposableBean {

  private final SslBundles bundles;

  private final ObservationRegistry registry;

  private final MeterRegistry meterRegistry;

  private final List<CloseableHttpClient> clients = new CopyOnWriteArrayList<>();

  /**
   * @param bundles to use
   * @param registry for observations
   * @param meterRegistry for connection pool metrics
   */
  public RestClientFactory(final SslBundles bundles, final ObservationRegistry registry,
      final MeterRegistry meterRegistry) {
    this.bundles = bundles;
    this.registry = registry;
    this.meterRegistry = meterRegistry;
  }

  /**
   * @param property for rest client
   * @return new instance
   */
  public RestClient create(final RestClientProperty property) {
    property.validateClient("rest-client[%s]".formatted(property.getName()));
    final RestClientProperty.Pool pool = property.getPool();
    final RestClientProperty.Timeouts timeouts = property.getTimeouts();

    final PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(pool.getMaxTotal())
            .setMaxConnPerRoute(pool.getMaxPerRoute())
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(timeouts.getConnect()))
                .setSocketTimeout(Timeout.of(timeouts.getSocket()))
                .setTimeToLive(TimeValue.of(pool.getTimeToLive()))
                .setValidateAfterInactivity(TimeValue.of(pool.getValidateAfterInactivity()))
                .build())
            .setDefaultTlsConfig(TlsConfig.custom()
                .setHandshakeTimeout(Timeout.of(timeouts.getTlsHandshake()))
                .build());

    this.trustStore(property).ifPresent(bundle -> connectionManagerBuilder
        .setTlsSocketStrategy(new DefaultClientTlsStrategy(bundle.createSslContext())));

    final PoolingHttpClientConnectionManager connectionManager = connectionManagerBuilder.build();
    this.registerPoolMetrics(property.getName(), connectionManager);

    final HttpClientBuilder httpClientBuilder = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.of(timeouts.getConnectionRequest()))
            .setResponseTimeout(Timeout.of(timeouts.getResponse()))
            .build())
        .disableCookieManagement();

    if (property.getMaxRetries() > 0) {
      httpClientBuilder.setRetryStrategy(new DefaultHttpRequestRetryStrategy(property.getMaxRetries(),
          TimeValue.of(property.getRetryInterval())));
    }
    else {
      httpClientBuilder.disableAutomaticRetries();
    }
    if (!property.isFollowRedirects()) {
      httpClientBuilder.disableRedirectHandling();
    }
    if (!pool.getEvictIdleAfter().isZero()) {
      httpClientBuilder.evictIdleConnections(TimeValue.of(pool.getEvictIdleAfter()));
    }

    final CloseableHttpClient httpClient = httpClientBuilder.build();
    this.clients.add(httpClient);

    log.info("Client: {} was created with pool max-total={} max-per-route={}, timeouts connection-request={} "
            + "connect={} tls-handshake={} response={} socket={}, worst case per request {}",
        property.getName(), pool.getMaxTotal(), pool.getMaxPerRoute(), timeouts.getConnectionRequest(),
        timeouts.getConnect(), timeouts.getTlsHandshake(), timeouts.getResponse(), timeouts.getSocket(),
        timeouts.worstCase());

    final RestClient.Builder restClientBuilder = RestClient.builder()
        .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

    Optional.ofNullable(property.getBaseUri())
        .ifPresent(restClientBuilder::baseUrl);

    return restClientBuilder
        .observationRegistry(this.registry)
        .build();
  }

  /**
   * Resolves the trust store of a client. A client without one, or with the bundle name {@code default}, uses the
   * trust store of the JVM.
   *
   * @param property for rest client
   * @return the bundle to use, or empty to use the default trust store
   */
  private Optional<SslBundle> trustStore(final RestClientProperty property) {
    final String bundleName = property.getTrustStoreBundleName();
    if (bundleName == null || "default".equals(bundleName)) {
      log.info("Client: {} was created without a trust-store, using default ...", property.getName());
      return Optional.empty();
    }
    return Optional.of(this.bundles.getBundle(bundleName));
  }

  /**
   * Registers gauges for the connection pool of a client. A pool that runs out of connections shows up as leased at
   * the maximum and a growing number of pending leases, which is otherwise only visible as slow requests.
   *
   * @param name of the client
   * @param connectionManager of the client
   */
  private void registerPoolMetrics(final String name, final PoolingHttpClientConnectionManager connectionManager) {
    this.poolGauge(name, "leased", connectionManager, PoolStats::getLeased);
    this.poolGauge(name, "available", connectionManager, PoolStats::getAvailable);
    this.poolGauge(name, "pending", connectionManager, PoolStats::getPending);
    this.poolGauge(name, "max", connectionManager, PoolStats::getMax);
  }

  private void poolGauge(final String name, final String state,
      final PoolingHttpClientConnectionManager connectionManager, final ToDoubleFunction<PoolStats> value) {
    Gauge.builder("oidf.http.client.pool", connectionManager,
            manager -> value.applyAsDouble(manager.getTotalStats()))
        .tag("client", name)
        .tag("state", state)
        .description("Connections of the outbound HTTP connection pool of a client")
        .register(this.meterRegistry);
  }

  @Override
  public void destroy() {
    this.clients.forEach(client -> {
      try {
        client.close();
      }
      catch (final IOException e) {
        log.warn("Failed to close http client: {}", e.getMessage());
      }
    });
    this.clients.clear();
  }
}
