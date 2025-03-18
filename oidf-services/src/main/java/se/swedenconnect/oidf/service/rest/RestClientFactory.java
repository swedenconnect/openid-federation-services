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
package se.swedenconnect.oidf.service.rest;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Optional;

/**
 * Factory class for creating rest clients.
 *
 * @author Felix Hellman
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
   * @param property for rest client
   * @return new instance
   */
  public RestClient create(final RestClientProperties.RestClientProperty property) {
    final HttpClient.Builder builder = HttpClient.newBuilder();

    Optional.ofNullable(property.getTrustStoreBundleName())
        .ifPresentOrElse(bundleName -> {
              final SslBundle bundle = this.bundles.getBundle(bundleName);
              builder.sslContext(bundle.createSslContext());
            },
            () -> log.info("Client: %s was created without a trust-store, using default ..."
                .formatted(property.getName())));

    final RestClient.Builder restClientBuilder = RestClient.builder()
        .requestFactory(new JdkClientHttpRequestFactory(builder.build()));

    Optional.ofNullable(property.getBaseUri())
        .ifPresent(restClientBuilder::baseUrl);

    return restClientBuilder
        .observationRegistry(this.registry)
        .build();
  }
}