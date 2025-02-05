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
package se.digg.oidfed.common.entity.integration.registry;

import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;
import se.digg.oidfed.common.entity.integration.Expirable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Refresh Ahead implementation of registry integration.
 * Will only use cached values when responding to requests.
 * Values are refreshed by calling {@link RefreshAheadRecordRegistrySource#refresh()}.
 *
 * @author Felix Hellman
 */
public class RefreshAheadRecordRegistrySource implements RefreshAheadLoader {

  private final RegistryProperties properties;
  private final FailableRecordRegistryIntegration integration;
  private final RegistryRefreshAheadCache cache;

  /**
   * Constructor.
   * @param properties for registry
   * @param integration for registry
   * @param cache for registry
   */
  public RefreshAheadRecordRegistrySource(
      final RegistryProperties properties,
      final FailableRecordRegistryIntegration integration,
      final RegistryRefreshAheadCache cache) {
    this.properties = properties;
    this.integration = integration;
    this.cache = cache;
  }

  /**
   * @param id of policy
   * @return policy if present
   */
  public Optional<PolicyRecord> getPolicy(final String id) {
    final PolicyRecord policy = this.cache.getPolicy(id);
    final Optional<PolicyRecord> cachedPolicy = Optional.ofNullable(policy);
    if (cachedPolicy.isPresent()) {
      return cachedPolicy;
    }
    return this.properties.policyRecords().stream()
        .filter(p -> p.getId().equals(id))
        .findFirst();
  }

  /**
   * @return list of Trust Mark Issuers from both registry and locally configured
   */
  public List<TrustMarkIssuerProperties> getTrustMarkIssuerProperties() {
    return Stream.concat(
        this.cache.getModules(this.properties.instanceId())
            .getTrustMarkIssuers().stream().map(TrustMarkIssuerModuleResponse::toProperties),
        this.properties.trustMarkIssuerProperties().stream()
    ).toList();
  }

  /**
   * @return list of trust anchor from both registry and locally configured
   */
  public List<TrustAnchorProperties> getTrustAnchorProperties() {
    return Stream.concat(
        this.cache.getModules(this.properties.instanceId())
            .getTrustAnchors().stream().map(TrustAnchorModuleResponse::toProperties),
        this.properties.trustAnchorProperties().stream()
    ).toList();
  }

  /**
   * @return list of resolver from both registry and locally configured
   */
  public List<ResolverProperties> getResolverProperties() {
    return Stream.concat(
        this.cache.getModules(this.properties.instanceId())
            .getResolvers().stream().map(ResolverModuleResponse::toProperty),
        this.properties.resolverProperties().stream()
    ).toList();
  }

  /**
   * @return list of all entities
   */
  public List<EntityRecord> getAllEntities() {
    final Stream<EntityRecord> cachedEntityRecords = this.getIssuers()
        .stream()
        .flatMap(issuer -> this.cache.getEntities(issuer).stream());

    return Stream.concat(
        this.properties.entityRecords().stream(),
        cachedEntityRecords
    ).toList();
  }

  @Override
  public void refresh() {
    if (this.cache.modulesNeedsRefresh(this.properties.instanceId())) {
      this.refreshModules();
      this.refreshEntities();
      this.refreshTrustMarkSubjects();
      this.refreshPolicies();
    }
  }

  private void refreshTrustMarkSubjects() {
    Stream.concat(
        this.cache.getModules(this.properties.instanceId())
            .getTrustMarkIssuers().stream()
            .map(TrustMarkIssuerModuleResponse::toProperties),
        this.properties.trustMarkIssuerProperties().stream()
    ).forEach(tmi -> {
      tmi.trustMarks().forEach(tm -> {
        final String issuer = tmi.issuerEntityId().getValue();
        final String trustMarkId = tm.trustMarkId().getTrustMarkId();
        final Expirable<List<TrustMarkSubject>> subjects = this.integration.getTrustMarkSubject(
            issuer,
            trustMarkId
        ).onFailure(() -> {
        }).orElseGet(() -> null);
        this.cache.registerTrustMarkSubjects(new TrustMarkSubjectKey(issuer, trustMarkId), subjects);
      });
    });
  }

  private void refreshPolicies() {
    this.getIssuers().stream()
        .flatMap(issuer -> this.cache.getEntities(issuer).stream())
        .map(EntityRecord::getPolicyRecordId)
        .map(this.integration::getPolicy)
        .map(f -> f.onFailure(() -> {
        }))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(policy ->
            this.cache.registerPolicy(policy.getValue().getId(), policy)
        );
  }

  private void refreshEntities() {
    final ArrayList<String> issuers = this.getIssuers();
    issuers.forEach(issuer -> {
      this.integration.getEntityRecords(issuer)
          .onFailure(() -> {
          })
          .ifPresent(entityRecords -> this.cache.registerIssuerEntities(issuer, entityRecords));
    });
  }

  private ArrayList<String> getIssuers() {
    final List<String> propertyIssuers = Stream.of(
            this.properties.resolverProperties()
                .stream().map(ResolverProperties::entityIdentifier),
            this.properties.trustAnchorProperties()
                .stream().map(TrustAnchorProperties::getEntityId)
                .map(Identifier::getValue),
            this.properties.trustMarkIssuerProperties()
                .stream().map(TrustMarkIssuerProperties::issuerEntityId)
                .map(Identifier::getValue)
        ).reduce(Stream::concat)
        .orElseGet(Stream::empty)
        .toList();

    final ArrayList<String> issuers = new ArrayList<>(propertyIssuers);

    Optional.ofNullable(this.cache.getModules(this.properties.instanceId()))
        .map(ModuleResponse::getIssuers)
        .ifPresent(issuers::addAll);
    return issuers;
  }


  private void refreshModules() {
    try {
      this.integration
          .getModules(this.properties.instanceId())
          .onFailure(() -> {
          })
          .ifPresent(modules -> this.cache.registerModule(this.properties.instanceId(), modules));
    } catch (final RegistryResponseException e) {
      throw new RuntimeException("Failed to update cache from registry");
    }
  }

  /**
   * @param issuer of trust mark
   * @param id of trust mark
   * @return list of subject
   */
  public List<TrustMarkSubject> getTrustMarkSubjects(final EntityID issuer, final TrustMarkId id) {
    return this.cache.getTrustMarkSubjects(new TrustMarkSubjectKey(issuer.getValue(), id.getTrustMarkId()));
  }

  /**
   * @param issuer of trust mark
   * @param id of trust mark
   * @param subject of trust mark
   * @return subject if subject exists
   */
  public Optional<TrustMarkSubject> getTrustMarkSubject(
      final EntityID issuer,
      final TrustMarkId id,
      final EntityID subject) {

    return this.getTrustMarkSubjects(issuer, id)
        .stream()
        .filter(tms -> tms.sub().
            equals(subject.getValue()))
        .findFirst();
  }
}
