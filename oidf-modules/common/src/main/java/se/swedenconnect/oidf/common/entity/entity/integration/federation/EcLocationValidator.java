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
package se.swedenconnect.oidf.common.entity.entity.integration.federation;

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates {@code ec_location} values as defined by OpenID Federation Entity Configuration Hosting 1.0, Section 2.
 * A value must be an {@code https} URL or a data URL with the media type {@code application/entity-statement+jwt}.
 * The {@code http} scheme is only accepted when explicitly allowed, which is meant for local test and demo setups.
 *
 * @author Martin Lindström
 */
@Slf4j
public class EcLocationValidator {

  /** Name of the claim. */
  public static final String CLAIM_NAME = "ec_location";

  /** Prefix of an {@code ec_location} data URL. */
  public static final String DATA_URL_PREFIX = "data:application/entity-statement+jwt,";

  private final boolean allowHttp;

  /**
   * Constructor.
   *
   * @param allowHttp whether {@code http} URLs are accepted
   */
  public EcLocationValidator(final boolean allowHttp) {
    this.allowHttp = allowHttp;
  }

  /**
   * Validates an {@code ec_location} value, as found in a Subordinate Statement.
   *
   * @param value the value
   * @throws IllegalArgumentException if the value is not allowed
   */
  public void validate(final String value) throws IllegalArgumentException {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("ec_location is empty");
    }
    if (isDataUrl(value)) {
      try {
        SignedJWT.parse(getDataUrlContent(value));
        return;
      }
      catch (final ParseException e) {
        throw new IllegalArgumentException("ec_location data URL does not hold a signed JWT");
      }
    }
    final URI uri;
    try {
      uri = new URI(value);
    }
    catch (final URISyntaxException e) {
      throw new IllegalArgumentException("ec_location '%s' is not a valid URL".formatted(value));
    }
    final boolean allowedScheme = "https".equalsIgnoreCase(uri.getScheme())
        || this.allowHttp && "http".equalsIgnoreCase(uri.getScheme());
    if (!allowedScheme) {
      throw new IllegalArgumentException(("ec_location '%s' must be an https URL or a data URL with the media "
          + "type application/entity-statement+jwt").formatted(value));
    }
    if (uri.getHost() == null) {
      throw new IllegalArgumentException("ec_location '%s' has no host".formatted(value));
    }
  }

  /**
   * Validates the {@code ec-location} of an entity record.
   *
   * @param entity the entity record
   * @throws IllegalArgumentException if the value is not allowed
   */
  public void validate(final EntityRecord entity) throws IllegalArgumentException {
    if (entity.getEcLocation() != null) {
      this.validate(resolve(entity.getEcLocation(), entity.getPreferedEntityId().getValue()));
    }
  }

  /**
   * Validates the {@code ec_location} that is issued for a subordinate.
   *
   * @param subordinate the subordinate
   * @throws IllegalArgumentException if the value is not allowed
   */
  public void validate(final TrustAnchorProperties.SubordinateListingProperty subordinate)
      throws IllegalArgumentException {
    final String ecLocation = subordinate.resolveEcLocation();
    if (ecLocation != null) {
      this.validate(ecLocation);
    }
  }

  /**
   * Logs a warning for each subordinate of the given Trust Anchors that is issued an {@code ec_location} that is
   * marked as critical, or that is issued an {@code ec_location} although it is an Intermediate Entity (Section 2).
   *
   * @param trustAnchors the Trust Anchors
   * @param entities the entity records, used to find subordinates that are Intermediate Entities
   */
  public static void warnForDiscouragedUse(
      final List<TrustAnchorProperties> trustAnchors, final List<EntityRecord> entities) {
    for (final TrustAnchorProperties trustAnchor : trustAnchors) {
      for (final TrustAnchorProperties.SubordinateListingProperty subordinate :
          Optional.ofNullable(trustAnchor.getSubordinates()).orElseGet(List::of)) {
        if (subordinate.resolveEcLocation() == null) {
          continue;
        }
        final String subject = subordinate.getEntityIdentifier().getValue();
        if (subordinate.getCrit() != null && subordinate.getCrit().contains(CLAIM_NAME)) {
          log.warn("Subordinate Statement from {} about {} marks ec_location as critical, which entities without "
              + "support for it will reject", trustAnchor.getEntityIdentifier().getValue(), subject);
        }
        if (isIntermediate(subordinate, trustAnchors, entities)) {
          log.warn("Subordinate Statement from {} about {} has ec_location, which is recommended for Leaf Entities "
              + "only", trustAnchor.getEntityIdentifier().getValue(), subject);
        }
      }
    }
  }

  /**
   * Tells whether a value is an {@code ec_location} data URL.
   *
   * @param value the value
   * @return true if the value is a data URL
   */
  public static boolean isDataUrl(final String value) {
    return value != null && value.startsWith(DATA_URL_PREFIX);
  }

  /**
   * Gets the Entity Configuration held by an {@code ec_location} data URL.
   *
   * @param value the data URL
   * @return the Entity Configuration
   */
  public static String getDataUrlContent(final String value) {
    return value.substring(DATA_URL_PREFIX.length());
  }

  /**
   * Resolves a configured {@code ec-location}. A value starting with {@code /} is a path relative to the given
   * entity identifier; other values are returned as is.
   *
   * @param value the configured value
   * @param entityId the entity identifier a relative value is resolved against
   * @return the resolved value
   */
  public static String resolve(final String value, final String entityId) {
    if (value.startsWith("/")) {
      return (entityId.endsWith("/") ? entityId.substring(0, entityId.length() - 1) : entityId) + value;
    }
    return value;
  }

  /**
   * Tells whether a subordinate is an Intermediate Entity, judged by the metadata in its Subordinate Statement, its
   * entity record, or it being configured as a Trust Anchor.
   *
   * @param subordinate the subordinate
   * @param trustAnchors the configured Trust Anchors
   * @param entities the entity records
   * @return true if the subordinate is known to be an Intermediate Entity
   */
  private static boolean isIntermediate(final TrustAnchorProperties.SubordinateListingProperty subordinate,
      final List<TrustAnchorProperties> trustAnchors, final List<EntityRecord> entities) {
    final String subject = subordinate.getEntityIdentifier().getValue();
    if (hasFetchEndpoint(subordinate.getMetadata())) {
      return true;
    }
    if (trustAnchors.stream().anyMatch(ta -> subject.equals(ta.getEntityIdentifier().getValue()))) {
      return true;
    }
    return entities.stream()
        .filter(entity -> subject.equals(entity.getEntityIdentifier().getValue()))
        .anyMatch(entity -> entity.getFederationMetadata()
            .map(metadata -> metadata.containsKey("federation_fetch_endpoint"))
            .orElse(false));
  }

  /**
   * Tells whether metadata has {@code federation_fetch_endpoint} in its {@code federation_entity} metadata.
   *
   * @param metadata the metadata, may be null
   * @return true if the endpoint is present
   */
  private static boolean hasFetchEndpoint(final Map<String, Object> metadata) {
    return Optional.ofNullable(metadata)
        .map(m -> m.get("federation_entity"))
        .filter(Map.class::isInstance)
        .map(m -> ((Map<?, ?>) m).get("federation_fetch_endpoint"))
        .filter(Objects::nonNull)
        .isPresent();
  }
}
