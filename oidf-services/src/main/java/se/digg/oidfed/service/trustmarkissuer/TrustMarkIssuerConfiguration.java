/*
 * Copyright 2024 Sweden Connect
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
package se.digg.oidfed.service.trustmarkissuer;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.PolicyRecord;
import se.digg.oidfed.common.entity.integration.CachedRecordRegistrySource;
import se.digg.oidfed.common.entity.integration.InMemoryRecordRegistryCache;
import se.digg.oidfed.common.entity.integration.RecordRegistryCache;
import se.digg.oidfed.common.entity.integration.RecordRegistryIntegration;
import se.digg.oidfed.common.entity.integration.RecordRegistrySource;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.entity.EntityConfigurationProperties;
import se.digg.oidfed.service.entity.RestClientRecordIntegration;
import se.digg.oidfed.service.rest.RestClientRegistry;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuerSubject;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuerSubjectInMemLoader;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuerSubjectLoader;
import se.digg.oidfed.trustmarkissuer.TrustMarkProperties;
import se.digg.oidfed.trustmarkissuer.TrustMarkSubjectRecordVerifier;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Configuration for trust mark.
 *
 * @author Felix Hellman
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TrustMarkIssuerModuleProperties.class)
@ConditionalOnProperty(value = TrustMarkIssuerModuleProperties.PROPERTY_PATH + ".active", havingValue = "true")
public class TrustMarkIssuerConfiguration {

  @Bean
  List<TrustMarkIssuer> trustMarkIssuer(final TrustMarkIssuerModuleProperties properties,
      final EntityRecordRegistry entityRegistry,
      final TrustMarkIssuerSubjectLoader trustMarkIssuerSubjectLoader){


    final List<TrustMarkIssuerModuleProperties.TrustMarkIssuers> trustMarkIssuersProperties =
        Optional.ofNullable(properties.getTrustMarkIssuers())
            .orElseThrow(() -> new IllegalArgumentException("TrustMarkIssuers is empty. Check application properties"));

    return trustMarkIssuersProperties.stream()
        .map(tmi -> this.toTrustMarkProperties(tmi,entityRegistry,trustMarkIssuerSubjectLoader))
        .peek(TrustMarkProperties::validate)
        .map(TrustMarkIssuer::new)
        .toList();
  }



  private TrustMarkProperties toTrustMarkProperties(
      final TrustMarkIssuerModuleProperties.TrustMarkIssuers properties,
      final EntityRecordRegistry entityRegistry,
      final TrustMarkIssuerSubjectLoader trustMarkIssuerSubjectLoader) {

    final EntityID issuerEntityId = new EntityID(properties.entityIdentifier());

    final Supplier<JWK> signKey = () ->
    {
      final EntityRecord entityProperties = entityRegistry.getEntity(issuerEntityId)
          .orElseThrow(() ->
              new IllegalArgumentException("Missing matching entityid in entityregistry: '%s'"
                  .formatted(properties.entityIdentifier())));
      return entityProperties.getSignKey();
    };

    return TrustMarkProperties.builder()
        .issuerEntityId(issuerEntityId)
        .alias(properties.alias())
        .trustMarkValidityDuration(properties.trustMarkValidityDuration())
        .signKey(signKey)
        .trustMarks(properties.trustMarks().stream()
            .map(tmIssuer -> TrustMarkProperties.TrustMarkIssuerProperties
                .builder()
                .trustMarkId(tmIssuer.trustMarkId())
                .refUri(Optional.ofNullable(tmIssuer.refUri()))
                .logoUri(Optional.ofNullable(tmIssuer.logoUri()))
                .delegation(Optional.ofNullable(tmIssuer.delegation()))
                .trustMarkIssuerSubjectLoader(
                    delegatedLoader(tmIssuer.subjects().stream().map(tmiSubject ->
                        TrustMarkIssuerSubject.builder()
                            .sub(tmiSubject.sub())
                            .expires(Optional.ofNullable(tmiSubject.expires()))
                            .revoked(tmiSubject.revoked())
                            .granted(Optional.ofNullable(tmiSubject.granted()))
                            .build()).toList(),trustMarkIssuerSubjectLoader))
                .build()).toList())
            .build();
  }

  private TrustMarkIssuerSubjectLoader delegatedLoader(
      final List<TrustMarkIssuerSubject> trustMarkPropertiesList,
      final TrustMarkIssuerSubjectLoader loader2){

    return  (issuerEntityId, trustMarkId, subject) -> {
      final TrustMarkIssuerSubjectInMemLoader trustMarkIssuerSubjectInMemLoader =
          new TrustMarkIssuerSubjectInMemLoader(trustMarkPropertiesList);

      final List<TrustMarkIssuerSubject> trustMarkIssuerSubjects = new ArrayList<>();
      trustMarkIssuerSubjects.addAll(
          trustMarkIssuerSubjectInMemLoader.loadSubject(issuerEntityId, trustMarkId, subject));
      trustMarkIssuerSubjects.addAll(loader2.loadSubject(issuerEntityId, trustMarkId, subject));
      return trustMarkIssuerSubjects;
    };
  }


  @Bean
  @ConditionalOnProperty(value =  TrustMarkIssuerModuleProperties.PROPERTY_PATH + ".client")
  @Qualifier("trustmarksubject-record-integration-client")
  RestClient trustmarksubjectRecordIntegrationClient(final RestClientRegistry registry,
      final TrustMarkIssuerModuleProperties properties) {

    return registry.getClient(properties.getClient())
        .orElseThrow(() -> new IllegalArgumentException("Can not find client:'%s' in RestClientRegistry"
            .formatted(properties.getClient())));
  }

  @Bean
  @ConditionalOnProperty(value =  TrustMarkIssuerModuleProperties.PROPERTY_PATH + ".client")
  TMIRestClientRecordIntegration trustMarkSubjectRecordIntegration(
      final  @Qualifier("trustmarksubject-record-integration-client")  RestClient client,
      final  @Qualifier("trustmarksubject-record-verifier") TrustMarkSubjectRecordVerifier verifier) {
    return new TMIRestClientRecordIntegration(client, verifier);
  }

  @Bean
  @ConditionalOnProperty(value =  TrustMarkIssuerModuleProperties.PROPERTY_PATH + ".client")
  @Qualifier("trustmarksubject-record-verifier")
  TrustMarkSubjectRecordVerifier trustMarkSubjectRecordVerifier(
      final KeyRegistry registry,
      final TrustMarkIssuerModuleProperties properties) {
    return new TrustMarkSubjectRecordVerifier(registry.getSet(properties.getJwkAlias()));
  }

  @Bean
  @ConditionalOnMissingBean(TrustMarkIssuerSubjectLoader.class)
  TrustMarkIssuerSubjectLoader emptyTrustMarkIssuerSubjectLoader() {
    log.warn("Starting application without a connection to an registry");
    return (issuerEntityId, trustMarkId, subject) -> List.of();
  }


}
