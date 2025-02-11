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
package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.common.entity.integration.federation.TrustMarkListingRequest;
import se.digg.oidfed.common.entity.integration.registry.RefreshAheadRecordRegistrySource;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkIssuerProperties;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkSubject;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkId;
import se.digg.oidfed.common.exception.InvalidRequestException;
import se.digg.oidfed.common.exception.NotFoundException;
import se.digg.oidfed.common.exception.ServerErrorException;
import se.digg.oidfed.common.module.Submodule;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of Trust Mark Issuer.
 *
 * @author Felix Hellman
 * @author Per Fredrik Plars
 */
@Slf4j
public class TrustMarkIssuer implements Submodule {


  private final TrustMarkIssuerProperties trustMarkIssuerProperties;
  private final TrustMarkSigner signer;
  private final RefreshAheadRecordRegistrySource source;
  private final Clock clock;

  /**
   * Constructor.
   * @param trustMarkIssuerProperties
   * @param signer
   * @param source
   * @param clock for keeping time
   */
  public TrustMarkIssuer(
      final TrustMarkIssuerProperties trustMarkIssuerProperties,
      final TrustMarkSigner signer,
      final RefreshAheadRecordRegistrySource source,
      final Clock clock
  ) {
    this.trustMarkIssuerProperties = trustMarkIssuerProperties;
    this.signer = signer;
    this.source = source;
    this.clock = clock;
  }

  @Override
  public String getAlias() {
    return this.trustMarkIssuerProperties.alias();
  }

  /**
   * Listing all trustmarks that are valid for this trustmarkid filtered by subject if supplied
   *
   * @param request Request containing trustmarkid and subject
   * @return listing of trust mark subjects that are valid. If there is no subject found a empty list is returned.
   */
  public List<String> trustMarkListing(final TrustMarkListingRequest request)
      throws InvalidRequestException, NotFoundException {
    if (Objects.isNull(request)) {
      throw new InvalidRequestException("Request can not be null");
    }
    if (Objects.isNull(request.trustMarkId())) {
      throw new InvalidRequestException("Trust mark id can not be null");
    }
    final TrustMarkId id = TrustMarkId.validate(request.trustMarkId(), InvalidRequestException::new);
    final List<String> result = this.source.getTrustMarkSubjects(this.trustMarkIssuerProperties.issuerEntityId(), id)
        .stream()
        .filter(tms -> {
          if (Objects.nonNull(tms.expires())) {
            return tms.expires().isAfter(Instant.now(this.clock));
          }
          return true;
        })
        .map(TrustMarkSubject::sub)
        .toList();
    if (result.isEmpty()) {
      throw new NotFoundException("Could not find any subjects.");
    }
    if (Objects.nonNull(request.subject())) {
      return result.stream().filter(sub -> sub.equals(request.subject())).map(List::of).findFirst()
          .orElseGet(List::of);
    }
    return result;
  }

  /**
   * Validate the trust mark status.
   *
   * @param request For a TrustMark status check
   * @return True if trust mark is active
   */
  public Boolean trustMarkStatus(final TrustMarkStatusRequest request)
      throws NotFoundException, InvalidRequestException {
    if (request.issueTime() != null) {
      throw new InvalidRequestException("IssueTime parameter is not supported");
    }
    final TrustMarkId id = TrustMarkId.create(request.trustMarkId());
    final boolean exists = this.trustMarkIssuerProperties.trustMarks()
        .stream()
        .anyMatch(tm -> tm.trustMarkId().equals(id));

    if (!exists) {
      throw new NotFoundException("Could not find any trust mark with id %s".formatted(request.trustMarkId()));
    }

    final Optional<TrustMarkSubject> subject =
        this.source.getTrustMarkSubject(this.trustMarkIssuerProperties.issuerEntityId(), id,
            new EntityID(request.subject()));
    return subject.isPresent()
        && !subject.get().revoked()
        && Optional.ofNullable(subject.get().expires()).map(e -> e.isAfter(Instant.now(this.clock))).orElse(true);
  }

  /**
   * Creating a SignedTrustMark for this subject. https://openid.net/specs/openid-federation-1_0.html#section-8.6.1
   *
   * @param request TrustMarkId and Subject is mandatory
   * @return trust mark in a JWT
   */
  public String trustMark(final TrustMarkRequest request) throws ServerErrorException, NotFoundException {
    final TrustMarkIssuerProperties.TrustMarkProperties properties =
        this.trustMarkIssuerProperties.trustMarks().stream()
        .filter(tm -> request.trustMarkId().equals(tm.trustMarkId().getTrustMarkId()))
        .findFirst()
        .get();

    final Optional<TrustMarkSubject> subject =
        this.source.getTrustMarkSubject(this.trustMarkIssuerProperties.issuerEntityId(),
        TrustMarkId.create(request.trustMarkId()), new EntityID(request.subject()));
    if (subject.isEmpty()) {
      throw new NotFoundException("Could not find subject");
    }
    final TrustMarkSubject trustMarkSubject = subject.get();
    try {
      return this.signer.sign(this.trustMarkIssuerProperties, properties, trustMarkSubject).serialize();
    } catch (final ParseException | JOSEException e) {
      throw new ServerErrorException("Failed to sign trust mark", e);
    }
  }

  @Override
  public List<EntityID> getEntityIds() {
    return List.of(this.trustMarkIssuerProperties.issuerEntityId());
  }
}
