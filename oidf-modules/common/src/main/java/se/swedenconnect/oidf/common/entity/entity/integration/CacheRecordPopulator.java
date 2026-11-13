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
package se.swedenconnect.oidf.common.entity.entity.integration;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EcLocationValidator;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RecordRegistryIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Responsible for populating cache from registry.
 *
 * @author Felix Hellman
 * @author Martin Lindström
 */
@Slf4j
public class CacheRecordPopulator {
  private final CachedRecordSource source;
  private final RecordRegistryIntegration integration;
  private final UUID instanceId;
  private final EcLocationValidator ecLocationValidator;

  /**
   * Constructor.
   * @param source to populate
   * @param integration to get information from
   * @param instanceId key for fetching information
   * @param ecLocationValidator for validating ec-location values of the loaded records
   */
  public CacheRecordPopulator(
      final CachedRecordSource source,
      final RecordRegistryIntegration integration,
      final UUID instanceId,
      final EcLocationValidator ecLocationValidator) {

    this.source = source;
    this.integration = integration;
    this.instanceId = instanceId;
    this.ecLocationValidator = ecLocationValidator;
  }

  /**
   * @return state
   */
  public CompositeRecord reload() {
    final Expirable<List<EntityRecord>> loadedEntityRecords = this.integration.getEntityRecords(this.instanceId);
    final Expirable<List<EntityRecord>> entityRecords = new Expirable<>(
        loadedEntityRecords.getExpiration(),
        loadedEntityRecords.getIssuedAt(),
        Optional.ofNullable(loadedEntityRecords.getValue()).orElseGet(List::of).stream()
            .filter(this::hasValidEcLocation)
            .toList());
    final Expirable<ModuleRecord> modules = this.integration.getModules(this.instanceId);
    final List<TrustAnchorProperties> trustAnchors =
        Optional.ofNullable(modules.getValue()).map(ModuleRecord::getTrustAnchors).orElseGet(List::of);
    trustAnchors.forEach(ta -> ta.setSubordinates(Optional.ofNullable(ta.getSubordinates()).orElseGet(List::of)
        .stream()
        .filter(subordinate -> this.isValidSubordinate(ta, subordinate))
        .toList()));
    EcLocationValidator.warnForDiscouragedUse(trustAnchors, entityRecords.getValue());
    Optional.ofNullable(modules.getValue()).map(ModuleRecord::getTrustMarkIssuers).orElseGet(List::of)
        .forEach(TrustMarkIssuerProperties::warnForShortValidityDurations);
    final CompositeRecord compositeRecord = new CompositeRecord(entityRecords, modules);
    this.source.addRecord(compositeRecord);
    return compositeRecord;
  }

  /**
   * Checks the ec-location of an entity record from the registry.
   *
   * @param entity the entity record
   * @return true if the record has no ec-location or a valid one
   */
  private boolean hasValidEcLocation(final EntityRecord entity) {
    try {
      this.ecLocationValidator.validate(entity);
      return true;
    }
    catch (final IllegalArgumentException e) {
      log.error("Ignoring entity {} from registry: {}", entity.getEntityIdentifier().getValue(), e.getMessage());
      return false;
    }
  }

  /**
   * Checks a subordinate from the registry: its listing fields and the ec_location that would be issued for it.
   *
   * @param trustAnchor the Trust Anchor of the subordinate
   * @param subordinate the subordinate
   * @return true if the subordinate is valid
   */
  private boolean isValidSubordinate(
      final TrustAnchorProperties trustAnchor, final TrustAnchorProperties.SubordinateListingProperty subordinate) {
    try {
      subordinate.validate();
      this.ecLocationValidator.validate(subordinate);
      return true;
    }
    catch (final IllegalArgumentException e) {
      log.error("Ignoring subordinate {} of {} from registry: {}", subordinate.getEntityIdentifier().getValue(),
          trustAnchor.getEntityIdentifier().getValue(), e.getMessage());
      return false;
    }
  }

  /**
   * Tells whether the records should be reloaded from the registry. An integration that can tell whether the
   * registry has changes is asked; otherwise the records are reloaded when the cached records are old.
   *
   * @return true if the records should be reloaded
   */
  public boolean shouldRefresh() {
    return this.changes().orElseGet(this.source::shouldRefresh);
  }

  /**
   * @return true if the integration reports changes in the registry that have not been loaded yet; false if it
   *     reports none, cannot tell, or the registry cannot be reached
   */
  public boolean hasChanges() {
    return this.changes().orElse(false);
  }

  private Optional<Boolean> changes() {
    try {
      return this.integration.hasChanges(this.instanceId);
    }
    catch (final RuntimeException e) {
      log.warn("Could not check the registry for changes: {}", e.getMessage());
      return Optional.empty();
    }
  }
}
