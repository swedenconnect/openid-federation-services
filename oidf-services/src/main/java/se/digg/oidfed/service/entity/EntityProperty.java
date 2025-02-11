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
package se.digg.oidfed.service.entity;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.HostedRecord;
import se.digg.oidfed.common.entity.integration.registry.records.TrustMarkSource;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.JsonObjectProperty;
import se.digg.oidfed.service.keys.FederationKeys;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration property to be loaded from spring.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@Slf4j
public class EntityProperty {
  private String issuer;
  private String subject;
  private String policyName;
  private String overrideConfigurationLocation;
  private List<String> publicKeys;
  @NestedConfigurationProperty
  private HostedRecordProperty hostedRecord;

  /**
   * Properties for hosted records
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  public static class HostedRecordProperty {
    private JsonObjectProperty metadata;
    @NestedConfigurationProperty
    private List<TrustMarkSourceProperty> trustMarkSources;
    private List<String> authorityHints;

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
        return new TrustMarkSource(new EntityID(this.issuer), this.trustMarkId);
      }
    }
  }

  /**
   * Converts the properties to the correct format.
   *
   * @param registry to load keys from
   * @param keys for default keys
   * @return new instance
   */
  public EntityRecord toEntityRecord(final KeyRegistry registry, final FederationKeys keys) {

    JWKSet jwkSet = null;

    if (Objects.isNull(this.hostedRecord)) {
      Assert.isTrue(Objects.nonNull(this.publicKeys),
          "Public keys can not be null for a non-hosted entity %s %s".formatted(this.issuer, this.subject));
      Assert.isTrue(!this.publicKeys.isEmpty(),
          "Public keys can not be empty for a non-hosted entity %s %s".formatted(this.issuer, this.subject));
      jwkSet = registry.getSet(this.publicKeys).toPublicJWKSet();
    } else {
      if (Objects.nonNull(this.publicKeys) && !this.publicKeys.isEmpty()) {
        log.warn("Hosted record with issuer:{} subject:{} was configured with one or more public-key which will be " +
                "ignored.",
            this.issuer, this.subject);
      }
      jwkSet = keys.validationKeys().toPublicJWKSet();
    }

    return new EntityRecord(
        new EntityID(this.issuer),
        new EntityID(this.subject),
        this.policyName,
        jwkSet,
        this.overrideConfigurationLocation,
        this.hostedRecord(this.hostedRecord).orElse(null)
    );
  }

  private Optional<HostedRecord> hostedRecord(final HostedRecordProperty property) {
    if (property == null) {
      return Optional.empty();
    }

    return Optional.of(new HostedRecord(
        property.metadata.toJsonObject(),
        Optional.ofNullable(property.trustMarkSources)
            .map(tms -> tms.stream()
                .map(HostedRecordProperty.TrustMarkSourceProperty::toTrustMarkSource)
                .toList()
            ).orElse(List.of()),
        property.authorityHints
    ));
  }
}
