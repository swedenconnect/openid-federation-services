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
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.common.entity.integration.Expirable;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Refresh Ahead implementation of registry integration.
 * Will only use cached values when responding to requests.
 * Values are refreshed by calling {@link RefreshAheadRecordRegistrySource#refresh()}.
 *
 * @author Felix Hellman
 */
@Slf4j
public class RefreshAheadRecordRegistrySource implements RefreshAheadLoader {

  private final RegistryProperties properties;
  private final FailableRecordRegistryIntegration integration;
  private final RegistryRefreshAheadCache cache;

  /**
   * Constructor.
   *
   * @param properties  for registry
   * @param integration for registry
   * @param cache       for registry
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
    return Optional.ofNullable(this.properties.policyRecords()).orElse(List.of()).stream()
        .filter(p -> p.getId().equals(id))
        .findFirst();
  }

  /**
   * @return list of Trust Mark Issuers from both registry and locally configured
   */
  public List<TrustMarkIssuerProperties> getTrustMarkIssuerProperties() {
    return Stream.concat(
        this.getModules()
            .getTrustMarkIssuers().stream().map(TrustMarkIssuerModuleResponse::toProperties),
        Optional.ofNullable(this.properties.trustMarkIssuerProperties())
            .orElse(List.of()).stream()
    ).toList();
  }

  private ModuleResponse getModules() {
    return Optional.ofNullable(this.properties.instanceId())
        .flatMap(id -> Optional.ofNullable(this.cache.getModules(id)))
        .orElse(new ModuleResponse());
  }

  /**
   * @return list of trust anchor from both registry and locally configured
   */
  public List<TrustAnchorProperties> getTrustAnchorProperties() {
    return Stream.concat(
        this.getModules()
            .getTrustAnchors().stream().map(TrustAnchorModuleResponse::toProperties),
        Optional.ofNullable(this.properties.trustAnchorProperties())
            .orElse(List.of()).stream()
    ).toList();
  }

  /**
   * @return list of resolver from both registry and locally configured
   */
  public List<ResolverProperties> getResolverProperties() {
    return Stream.concat(
        this.getModules()
            .getResolvers().stream().map(ResolverModuleResponse::toProperty),
        Optional.ofNullable(this.properties.resolverProperties())
            .orElse(List.of()).stream()
    ).toList();
  }

  /**
   * @return list of all entities
   */
  public List<EntityRecord> getAllEntities() {
    final Stream<EntityRecord> cachedEntityRecords = this.getIssuers()
        .stream()
        .flatMap(issuer ->
            Optional.ofNullable(this.cache.getEntities(issuer)).orElse(List.of()).stream()
        );

    return Stream.concat(
        Optional.ofNullable(this.properties.entityRecords())
            .orElse(List.of()).stream(),
        cachedEntityRecords
    ).toList();
  }

  @Override
  public void refresh() {

    final Optional<UUID> instance = Optional.ofNullable(this.properties.instanceId());
    instance.ifPresent(id -> {
          if (this.cache.modulesNeedsRefresh(this.properties.instanceId())) {
            this.refreshModules();
            this.refreshEntities();
            this.refreshTrustMarkSubjects();
            this.refreshPolicies();
          }
        }
    );

  }

  private void refreshTrustMarkSubjects() {
    if (this.properties.enabled()) {
      Stream.concat(
          this.getModules()
              .getTrustMarkIssuers().stream()
              .map(TrustMarkIssuerModuleResponse::toProperties),
          Optional.ofNullable(this.properties.trustMarkIssuerProperties()).orElse(List.of()).stream()
      ).forEach(tmi -> {
        tmi.trustMarks().forEach(tm -> {
          final String issuer = tmi.issuerEntityId().getValue();
          final String trustMarkId = tm.trustMarkId().getTrustMarkId();
          final Expirable<List<TrustMarkSubjectRecord>> subjects = this.integration.getTrustMarkSubject(
              issuer,
              trustMarkId
          ).onFailure(() -> {
          }).orElseGet(() -> null);
          this.cache.registerTrustMarkSubjects(new TrustMarkSubjectKey(issuer, trustMarkId), subjects);
        });
      });
    }
  }

  private void refreshPolicies() {
    this.getIssuers().stream()
        .flatMap(issuer -> Optional.ofNullable(this.cache.getEntities(issuer)).orElse(List.of()).stream())
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
    if (this.properties.enabled()) {
      final ArrayList<String> issuers = this.getIssuers();
      issuers.forEach(issuer -> {
        this.integration.getEntityRecords(issuer)
            .onFailure(() -> {
            })
            .ifPresent(entityRecords -> this.cache.registerIssuerEntities(issuer, entityRecords));
      });
    }
  }

  private ArrayList<String> getIssuers() {
    final List<String> propertyIssuers = Stream.of(
            Optional.ofNullable(this.properties.resolverProperties())
                .orElse(List.of()).stream()
                .map(ResolverProperties::entityIdentifier),
            Optional.ofNullable(this.properties.trustAnchorProperties())
                .orElse(List.of()).stream()
                .map(TrustAnchorProperties::getEntityId)
                .map(Identifier::getValue),
            Optional.ofNullable(this.properties.trustMarkIssuerProperties())
                .orElse(List.of()).stream()
                .map(TrustMarkIssuerProperties::issuerEntityId)
                .map(Identifier::getValue)
        ).reduce(Stream::concat)
        .orElseGet(Stream::empty)
        .toList();

    final ArrayList<String> issuers = new ArrayList<>(propertyIssuers);
    issuers.addAll(this.getModules().getIssuers());
    return issuers;
  }


  private void refreshModules() {
    if (this.properties.enabled()) {
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
  }

  /**
   * @param issuer of trust mark
   * @param id     of trust mark
   * @return list of subject
   */
  public List<TrustMarkSubjectRecord> getTrustMarkSubjects(final EntityID issuer, final TrustMarkId id) {
    return Stream.concat(
        Optional.ofNullable(this.properties.trustMarkSubjectRecords()).orElse(List.of())
            .stream()
            .filter(sub -> sub.tmi().equals(id.trustMarkId) && sub.iss().equals(issuer.getValue())),
        Optional.ofNullable(this.cache.getTrustMarkSubjects(new TrustMarkSubjectKey(issuer.getValue(),
            id.getTrustMarkId()))).orElse(List.of()).stream()
    ).toList();
  }

  /**
   * @param issuer  of trust mark
   * @param id      of trust mark
   * @param subject of trust mark
   * @return subject if subject exists
   */
  public Optional<TrustMarkSubjectRecord> getTrustMarkSubject(
      final EntityID issuer,
      final TrustMarkId id,
      final EntityID subject) {


    return Optional.ofNullable(this.getTrustMarkSubjects(issuer, id)).orElse(List.of())
        .stream()
        .filter(tms -> tms.sub().
            equals(subject.getValue()))
        .findFirst();
  }
}
