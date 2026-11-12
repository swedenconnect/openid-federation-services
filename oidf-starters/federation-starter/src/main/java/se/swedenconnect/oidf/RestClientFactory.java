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
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

/**
 * Factory class for creating rest clients.
 *
 * @author Felix Hellman
 * @author Martin Lindström
 */
@Slf4j
public class RestClientFactory {

  private final SslBundles bundles;

  private final ObservationRegistry registry;

  /**
   * @param bundles to use
   * @param registry for observations
   */
  public RestClientFactory(final SslBundles bundles, final ObservationRegistry registry) {
    this.bundles = bundles;
    this.registry = registry;
  }

  /**
   * Creates a rest client. Timeouts that are not set in the property are taken from the supplied defaults.
   *
   * @param property for rest client
   * @param defaultTimeout timeouts to use for settings that are not set in the property
   * @return new instance
   */
  public @NonNull RestClient create(
      final @NonNull RestClientProperty property, final RestClientProperty.@NonNull Timeout defaultTimeout) {
    final Optional<RestClientProperty.Timeout> timeout = Optional.ofNullable(property.getTimeout());
    final Duration connectTimeout = timeout.map(RestClientProperty.Timeout::getConnect)
        .orElse(defaultTimeout.getConnect());
    final Duration readTimeout = timeout.map(RestClientProperty.Timeout::getRead)
        .orElse(defaultTimeout.getRead());

    final HttpClient.Builder builder = HttpClient.newBuilder();
    Optional.ofNullable(connectTimeout).ifPresent(builder::connectTimeout);

    Optional.ofNullable(property.getTrustStoreBundleName())
        .ifPresentOrElse(bundleName -> {
              if ("default".equals(bundleName)) {
                log.info("Client: %s was created without a trust-store, using default ..."
                    .formatted(property.getName()));
              } else {
                final SslBundle bundle = this.bundles.getBundle(bundleName);
                builder.sslContext(bundle.createSslContext());
              }
            },
            () -> log.info("Client: %s was created without a trust-store, using default ..."
                .formatted(property.getName())));

    final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(builder.build());
    Optional.ofNullable(readTimeout).ifPresent(requestFactory::setReadTimeout);

    final RestClient.Builder restClientBuilder = RestClient.builder()
        .requestFactory(requestFactory);

    Optional.ofNullable(property.getBaseUri())
        .ifPresent(restClientBuilder::baseUrl);

    return restClientBuilder
        .observationRegistry(this.registry)
        .build();
  }
}