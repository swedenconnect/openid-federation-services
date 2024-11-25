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
package se.digg.oidfed.service.entity;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.HostedRecord;
import se.digg.oidfed.common.entity.TrustMarkSource;
import se.digg.oidfed.common.keys.KeyRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration property to be loaded from spring.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class EntityProperty {
  private String issuer;
  private String subject;
  private String policyName;
  private String overrideConfigurationLocation;
  private List<String> jwkAlias;
  @NestedConfigurationProperty
  private HostedEntityProperty hostedRecord;
  private boolean isDefaultEntity;

  /**
   * Properties for hosted records
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  public static class HostedEntityProperty {
    private Map<String, Object> metadata;
    @NestedConfigurationProperty
    private List<TrustMarkSourceProperty> trustMarkSources;
    private List<String> authortyHints;

    /**
     * Property for trust mark source.
     *
     * @author Felix Hellman
     */
    @Getter
    @Setter
    public static class TrustMarkSourceProperty {
      private String issuer;
      private String trustMarkId;

      /**
       * @return converted property
       */
      public TrustMarkSource toTrustMarkSource() {
        return new TrustMarkSource(new EntityID(issuer), trustMarkId);
      }
    }
  }

  /**
   * Converts the properties to the correct format.
   * @param registry to load keys from
   * @return new instance
   */
  public EntityRecord toEntityRecord(final KeyRegistry registry) {
    return new EntityRecord(
        new EntityID(issuer),
        new EntityID(subject),
        policyName,
        new JWKSet(jwkAlias.stream().map(registry::getKey).map(Optional::get).toList()),
        overrideConfigurationLocation,
        hostedRecord(hostedRecord).orElse(null)
    );
  }

  private Optional<HostedRecord> hostedRecord(final HostedEntityProperty property) {
    if (property == null) {
      return Optional.empty();
    }
    return Optional.of(new HostedRecord(
        property.metadata,
        Optional.ofNullable(property.trustMarkSources)
            .map(tms -> tms.stream()
                .map(tm -> tm.toTrustMarkSource())
                .toList()
            ).orElse(List.of()),
        property.authortyHints
    ));
  }
}
