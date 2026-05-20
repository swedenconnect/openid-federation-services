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
package se.swedenconnect.oidf.common.entity.entity.integration.registry.records;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.ExclusionStrategy;
import com.nimbusds.jose.shaded.gson.FieldAttributes;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data class for entity record.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class EntityRecord implements Serializable {
  public static final ExclusionStrategy EXCLUSION_STRATEGY = new ExclusionStrategy() {
    @Override
    public boolean shouldSkipField(final FieldAttributes fieldAttributes) {
      return fieldAttributes.getDeclaringClass().equals(KeyStore.class);
    }

    @Override
    public boolean shouldSkipClass(final Class<?> aClass) {
      return aClass.equals(KeyStore.class);
    }
  };

  @SerializedName("entity-identifier")
  private EntityID entityIdentifier;
  @SerializedName("virtual-entity-id")
  private EntityID virtualEntityId;
  @SerializedName("metadata")
  private Map<String, Object> metadata;
  @SerializedName("trust-mark-source")
  private List<TrustMarkSourceProperty> trustMarkSource;
  @SerializedName("ec-location")
  private String ecLocation;
  @SerializedName("authority-hints")
  private List<String> authorityHints;
  @SerializedName("jwks")
  private JWKSet jwks;
  @SerializedName("crit")
  private List<String> crit;

  /**
   * @return federation entity metadata nested inside the metadata map under the "federation_entity" key
   */
  @SuppressWarnings("unchecked")
  public Optional<Map<String, Object>> getFederationMetadata() {
    if (this.metadata == null) {
      return Optional.empty();
    }
    return Optional.ofNullable((Map<String, Object>) this.metadata.get("federation_entity"));
  }

  /**
   * @return federation_fetch_endpoint constructed from virtual-entity-id (preferred) or entity-identifier + value
   */
  public Optional<String> getFederationFetchEndpoint() {
    return this.getFederationEndpoint("federation_fetch_endpoint");
  }

  /**
   * @return federation_resolve_endpoint constructed from virtual-entity-id (preferred) or entity-identifier + value
   */
  public Optional<String> getFederationResolveEndpoint() {
    return this.getFederationEndpoint("federation_resolve_endpoint");
  }

  /**
   * @return federation_list_endpoint constructed from virtual-entity-id (preferred) or entity-identifier + value
   */
  public Optional<String> getFederationListEndpoint() {
    return this.getFederationEndpoint("federation_list_endpoint");
  }

  /**
   * @return federation_trust_mark_status_endpoint constructed from virtual-entity-id (preferred)
   *     or entity-identifier + value
   */
  public Optional<String> getFederationTrustMarkStatusEndpoint() {
    return this.getFederationEndpoint("federation_trust_mark_status_endpoint");
  }

  /**
   * @return federation_trust_marked_entities_endpoint constructed from virtual-entity-id (preferred)
   *     or entity-identifier + value
   */
  public Optional<String> getFederationTrustMarkedEntitiesEndpoint() {
    return this.getFederationEndpoint("federation_trust_marked_entities_endpoint");
  }

  /**
   * @return federation_trust_mark_endpoint constructed from virtual-entity-id (preferred) or entity-identifier + value
   */
  public Optional<String> getFederationTrustMarkEndpoint() {
    return this.getFederationEndpoint("federation_trust_mark_endpoint");
  }

  /**
   * @return federation_historical_keys_endpoint constructed from virtual-entity-id (preferred)
   *     or entity-identifier + value
   */
  public Optional<String> getFederationHistoricalKeysEndpoint() {
    return this.getFederationEndpoint("federation_historical_keys_endpoint");
  }

  /**
   * @return the virtual entity id if present, otherwise the entity identifier
   */
  public EntityID getPreferedEntityId() {
    return this.virtualEntityId != null ? this.virtualEntityId : this.entityIdentifier;
  }

  /**
   * @return list of entity configuration endpoints for this entity
   */
  public List<String> getEntityConfigurationEndpoints() {
    final EntityID entityID = this.virtualEntityId != null ? this.virtualEntityId : this.entityIdentifier;
    final List<String> endpoints = new ArrayList<>();
    if (this.ecLocation != null) {
      if (this.ecLocation.startsWith("http:") || this.ecLocation.startsWith("https://")) {
        endpoints.add(this.ecLocation);
      }
      if (this.ecLocation.startsWith("/")) {
        endpoints.add(entityID.getValue() + this.ecLocation);
      }
    }
    endpoints.add(this.entityIdentifier.getValue() + "/.well-known/openid-federation");
    return endpoints;
  }

  private Optional<String> getFederationEndpoint(final String key) {
    return this.getFederationMetadata()
        .map(m -> (String) m.get(key))
        .map(value -> {
          if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
          }
          final EntityID base = this.virtualEntityId != null ? this.virtualEntityId : this.entityIdentifier;
          return base.getValue() + value;
        });
  }

  /**
   * Returns the federation trust mark listing endpoint if configured.
   *
   * @return optional endpoint URL
   */
  public Optional<String> getFederationTrustMarkListingEndpoint() {
    return this.getFederationEndpoint("federation_trust_mark_list_endpoint");
  }
}
