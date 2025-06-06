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
package se.swedenconnect.oidf.trustmarkissuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkId;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectRecord;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.exception.ServerErrorException;

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
public class TrustMarkIssuer {


  private final TrustMarkIssuerProperties trustMarkIssuerProperties;
  private final TrustMarkSigner signer;
  private final CompositeRecordSource source;
  private final Clock clock;

  /**
   * Constructor.
   *
   * @param trustMarkIssuerProperties
   * @param signer
   * @param source
   * @param clock                     for keeping time
   */
  public TrustMarkIssuer(
      final TrustMarkIssuerProperties trustMarkIssuerProperties,
      final TrustMarkSigner signer,
      final CompositeRecordSource source,
      final Clock clock
  ) {
    this.trustMarkIssuerProperties = trustMarkIssuerProperties;
    this.signer = signer;
    this.source = source;
    this.clock = clock;
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
        .map(TrustMarkSubjectRecord::sub)
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

    final Optional<TrustMarkSubjectRecord> subject =
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

    final TrustMarkProperties properties =
        this.trustMarkIssuerProperties.trustMarks().stream()
        .filter(tm -> request.trustMarkId().equals(tm.trustMarkId().getTrustMarkId()))
        .findFirst()
        .get();

    final Optional<TrustMarkSubjectRecord> subject =
        this.source.getTrustMarkSubject(this.trustMarkIssuerProperties.issuerEntityId(),
        TrustMarkId.create(request.trustMarkId()), new EntityID(request.subject()));
    if (subject.isEmpty()) {
      throw new NotFoundException("Could not find subject");
    }
    final TrustMarkSubjectRecord trustMarkSubjectRecord = subject.get();
    try {
      return this.signer.sign(this.trustMarkIssuerProperties, properties, trustMarkSubjectRecord).serialize();
    } catch (final ParseException | JOSEException e) {
      throw new ServerErrorException("Failed to sign trust mark", e);
    }
  }


  /**
   * @return entity id of this trust mark issuer.
   */
  public EntityID getEntityId() {
    return this.trustMarkIssuerProperties.issuerEntityId();
  }
}
