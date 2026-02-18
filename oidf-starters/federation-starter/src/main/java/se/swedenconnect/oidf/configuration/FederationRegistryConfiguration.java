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
package se.swedenconnect.oidf.configuration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.ExclusionStrategy;
import com.nimbusds.jose.shaded.gson.FieldAttributes;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.CacheFactory;
import se.swedenconnect.oidf.FederationProperties;
import se.swedenconnect.oidf.InMemoryCacheFactory;
import se.swedenconnect.oidf.JWSRegistryVerifier;
import se.swedenconnect.oidf.RestClientFactory;
import se.swedenconnect.oidf.RestClientRecordIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.Cache;
import se.swedenconnect.oidf.common.entity.entity.integration.CacheRecordPopulator;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.DurationDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.InstantDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSKidReferenceLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSSerializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JsonRegistryLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.EntityRecordDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RecordRegistryIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryVerifier;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Registry Configuration.
 *
 * @author Felix Hellman
 */
@Configuration
public class FederationRegistryConfiguration {
  @Bean
  @ConditionalOnMissingBean(CacheFactory.class)
  CacheFactory inMemoryCacheFactory(final Clock clock) {
    return new InMemoryCacheFactory(clock);
  }

  @Bean
  Cache<String, CompositeRecord> compositeRecordCache(final CacheFactory cacheFactory) {
    return cacheFactory.create(CompositeRecord.class);
  }

  @Bean
  CachedRecordSource cachedRecordSource(final Cache<String, CompositeRecord> cache) {
    return new CachedRecordSource(cache);
  }

  @Bean
  JWKSKidReferenceLoader jwksKidReferenceLoader(final KeyRegistry registry) {
    return new JWKSKidReferenceLoader(registry);
  }

  @Bean
  Gson registryGson(final JWKSKidReferenceLoader loader, final KeyRegistry registry) {
    return new GsonBuilder()
        .registerTypeAdapter(Duration.class, new DurationDeserializer())
        .registerTypeAdapter(Instant.class, new InstantDeserializer())
        .registerTypeAdapter(EntityID.class, new EntityIdentifierDeserializer())
        .registerTypeAdapter(TrustMarkType.class, new TrustMarkIdentifierDeserializer())
        .registerTypeAdapter(JWKSet.class, new JWKSSerializer(loader, loader))
        .registerTypeAdapter(CompositeRecord.class, new CompositeRecordSerializer())
        .registerTypeAdapter(EntityRecord.class, new EntityRecordDeserializer(loader, registry))
        .create();
  }

  @Bean
  JsonRegistryLoader jsonRegistryLoader(final Gson gson) {
    return new JsonRegistryLoader(gson);
  }

  @Bean
  RegistryVerifier registryVerifier(final FederationProperties properties, final JsonRegistryLoader loader) {
    return new JWSRegistryVerifier(properties.getRegistry().getIntegration().getValidationKeys(), loader);
  }

  @Bean
  RestClientFactory restClientFactory(final SslBundles sslBundle, final ObservationRegistry registry) {
    return new RestClientFactory(sslBundle, registry);
  }

  @Bean
  RestClient registryRestClient(final RestClientFactory restClientFactory,
                                final FederationProperties properties) {
    return restClientFactory.create(properties.getRegistry().getIntegration().getClient());
  }

  @Bean
  RecordRegistryIntegration recordRegistryIntegration(
      final RegistryVerifier verifier,
      @Qualifier("registryRestClient") final RestClient restClient) {

    return new RestClientRecordIntegration(verifier, restClient);
  }

  @Bean
  CacheRecordPopulator cacheRecordPopulator(
      final CachedRecordSource source,
      final RecordRegistryIntegration integration,
      final FederationProperties properties
  ) {
    return new CacheRecordPopulator(source, integration, properties.getRegistry().getIntegration().getInstanceId());
  }

  @Bean
  Clock systemClock() {
    return Clock.system(ZoneId.systemDefault());
  }
}
