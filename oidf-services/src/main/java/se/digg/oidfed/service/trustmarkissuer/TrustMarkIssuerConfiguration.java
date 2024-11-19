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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.common.entity.EntityProperties;
import se.digg.oidfed.common.entity.EntityRegistry;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuerSubject;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuerSubjectInMemLoader;
import se.digg.oidfed.trustmarkissuer.TrustMarkProperties;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for trust mark.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(TrustMarkIssuerModuleProperties.class)
@ConditionalOnProperty(value = TrustMarkIssuerModuleProperties.PROPERTY_PATH + ".active", havingValue = "true")
public class TrustMarkIssuerConfiguration {

  @Bean
  List<TrustMarkIssuer> trustMarkIssuer(TrustMarkIssuerModuleProperties properties, KeyRegistry keyRegistry,
      EntityRegistry entityRegistry){


    final List<TrustMarkIssuerModuleProperties.TrustMarkIssuers> trustMarkIssuersProperties =
        Optional.ofNullable(properties.getTrustMarkIssuers())
            .orElseThrow(() -> new IllegalArgumentException("TrustMarkIssuers is empty. Check application properties"));

    return trustMarkIssuersProperties.stream()
        .map(tmi -> toTrustMarkProperties(tmi,entityRegistry))
        .peek(TrustMarkProperties::validate)
        .map(TrustMarkIssuer::new)
        .toList();
  }

  private TrustMarkProperties toTrustMarkProperties(TrustMarkIssuerModuleProperties.TrustMarkIssuers properties,
      EntityRegistry entityRegistry) {
    final EntityID issuerEntityId = new EntityID(properties.entityIdentifier());
    final EntityProperties entityProperties = entityRegistry.getEntity(issuerEntityId)
        .orElseThrow(() ->
            new IllegalArgumentException("Missing matching entityid in entityregistry: '%s'"
                .formatted(properties.entityIdentifier())));



    return TrustMarkProperties.builder()
        .issuerEntityId(issuerEntityId)
        .alias(properties.alias())
        .trustMarkValidityDuration(properties.trustMarkValidityDuration())
        .signKey(entityProperties.getSignKey())
        .trustMarks(properties.trustMarks().stream()
            .map(tmIssuer -> TrustMarkProperties.TrustMarkIssuerProperties
                .builder()
                .trustMarkId(tmIssuer.trustMarkId())
                .refUri(Optional.ofNullable(tmIssuer.refUri()))
                .logoUri(Optional.ofNullable(tmIssuer.logoUri()))
                .delegation(Optional.ofNullable(tmIssuer.delegation()))
                .trustMarkIssuerSubjectLoader(
                    new TrustMarkIssuerSubjectInMemLoader(tmIssuer.subjects().stream().map(tmiSubject ->
                        TrustMarkIssuerSubject.builder()
                            .sub(tmiSubject.sub())
                            .expires(Optional.ofNullable(tmiSubject.expires()))
                            .revoked(tmiSubject.revoked())
                            .granted(Optional.ofNullable(tmiSubject.granted()))
                            .build()).toList()))
                .build()).toList())
            .build();
  }
}
