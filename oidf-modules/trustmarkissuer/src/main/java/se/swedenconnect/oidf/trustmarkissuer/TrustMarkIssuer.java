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
package se.swedenconnect.oidf.trustmarkissuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.exception.ServerErrorException;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
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
    if (Objects.isNull(request.trustMarkType())) {
      throw new InvalidRequestException("Trust mark id can not be null");
    }
    final TrustMarkType id = TrustMarkType.validate(request.trustMarkType(), InvalidRequestException::new);
    final List<String> result = this.source.getTrustMarkSubjects(this.trustMarkIssuerProperties.entityIdentifier(), id)
        .stream()
        .filter(tms -> {
          if (Objects.nonNull(tms.expires())) {
            return tms.expires().isAfter(Instant.now(this.clock));
          }
          return true;
        })
        .map(TrustMarkSubjectProperty::sub)
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
   * Gets the status of a trust mark issued by this trust mark issuer (OpenID Federation 1.0, Section 8.4).
   *
   * @param request For a TrustMark status check
   * @return signed status response with the status {@code active}, {@code invalid}, {@code revoked} or
   *     {@code expired}
   * @throws NotFoundException if the trust mark was not issued by this issuer, or its type or subject is unknown
   */
  public String trustMarkStatus(final TrustMarkStatusRequest request)
      throws NotFoundException, InvalidRequestException {

    try {
      final SignedJWT parse = SignedJWT.parse(request.trustMark());

      final JWTClaimsSet trustMarkClaims = parse.getJWTClaimsSet();
      final String trustMarkType = EntityStatementClaims.getTrustMarkType(parse);
      final String sub = trustMarkClaims.getSubject();
      final String entityIdentifier = this.trustMarkIssuerProperties.entityIdentifier().getValue();

      if (!entityIdentifier.equals(trustMarkClaims.getIssuer())) {
        throw new NotFoundException("Trust mark was not issued by %s".formatted(entityIdentifier));
      }
      final boolean exists = trustMarkType != null && this.trustMarkIssuerProperties.trustMarks()
          .stream()
          .anyMatch(tmi -> tmi.getTrustMarkType().getTrustMarkType().equals(trustMarkType));
      if (!exists) {
        throw new NotFoundException("Could not find any trust mark with type %s".formatted(trustMarkType));
      }

      final EntityRecord entity = this.source.getEntity(new NodeKey(entityIdentifier))
          .orElseThrow(() -> new NotFoundException("Trust mark issuer %s not found".formatted(entityIdentifier)));
      final String status = this.resolveStatus(entity, request.trustMark(), trustMarkClaims, trustMarkType, sub);

      return this.signer.signStatus(entity, request.trustMark(), status).serialize();
    } catch (final java.text.ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Resolves the status of a trust mark whose issuer and type are known to be this issuer's.
   *
   * @param entity the trust mark issuer entity
   * @param trustMark the serialized trust mark
   * @param trustMarkClaims the claims of the trust mark
   * @param trustMarkType the trust mark type
   * @param sub the subject of the trust mark
   * @return the status
   * @throws NotFoundException if the subject is not registered for the trust mark type
   */
  private String resolveStatus(final EntityRecord entity, final String trustMark, final JWTClaimsSet trustMarkClaims,
      final String trustMarkType, final String sub) throws NotFoundException {
    if (!this.signer.verify(entity, trustMark)) {
      return "invalid";
    }
    if (sub == null) {
      throw new NotFoundException("Trust mark has no subject");
    }
    final TrustMarkSubjectProperty subject = this.source.getTrustMarkSubject(
            this.trustMarkIssuerProperties.entityIdentifier(), new TrustMarkType(trustMarkType), new EntityID(sub))
        .orElseThrow(() -> new NotFoundException("Subject %s has no trust mark of type %s"
            .formatted(sub, trustMarkType)));
    if (subject.revoked()) {
      return "revoked";
    }
    final Date expirationTime = trustMarkClaims.getExpirationTime();
    if (expirationTime != null && Instant.now(this.clock).isAfter(expirationTime.toInstant())) {
      return "expired";
    }
    return "active";
  }

  /**
   * Creating a SignedTrustMark for this subject. https://openid.net/specs/openid-federation-1_0.html#section-8.6.1
   *
   * @param request TrustMarkId and Subject is mandatory
   * @return trust mark in a JWT
   * @throws NotFoundException if the type or subject is unknown, or the subject is revoked, expired or not yet
   *     granted
   * @throws ServerErrorException if the trust mark could not be signed
   */
  public String trustMark(final TrustMarkRequest request) throws ServerErrorException, NotFoundException {

    final Optional<TrustMarkProperties> trustMarkProperties = this.trustMarkIssuerProperties.trustMarks().stream()
        .filter(tm -> request.trustMarkType().equals(tm.getTrustMarkType().getTrustMarkType()))
        .findFirst();
    if (trustMarkProperties.isEmpty()) {
      throw new NotFoundException("Trust mark type %s was not found for the trust mark issuer."
          .formatted(request.trustMarkType())
      );
    }
    final TrustMarkProperties properties =
        trustMarkProperties
            .get();

    final Optional<TrustMarkSubjectProperty> subject = properties.getTrustMarkSubjects()
        .stream()
        .filter(trustMarkSubjectRecord -> trustMarkSubjectRecord.sub().equals(request.subject()))
        .findFirst();
    if (subject.isEmpty()) {
      throw new NotFoundException("Could not find subject");
    }
    final TrustMarkSubjectProperty trustMarkSubjectProperty = subject.get();
    final Instant now = Instant.now(this.clock);
    if (trustMarkSubjectProperty.revoked()) {
      throw new NotFoundException("Trust mark for subject has been revoked");
    }
    if (trustMarkSubjectProperty.expires() != null && !now.isBefore(trustMarkSubjectProperty.expires())) {
      throw new NotFoundException("Trust mark for subject has expired");
    }
    if (trustMarkSubjectProperty.granted() != null && now.isBefore(trustMarkSubjectProperty.granted())) {
      throw new NotFoundException("Trust mark for subject is not yet granted");
    }
    try {
      final String entityIdentifier = this.trustMarkIssuerProperties.entityIdentifier().getValue();
      return this.signer.sign(this.source.getEntity(new NodeKey(entityIdentifier)).get(),
          this.trustMarkIssuerProperties, properties,
          trustMarkSubjectProperty).serialize();
    } catch (final ParseException | JOSEException e) {
      throw new ServerErrorException("Failed to sign trust mark", e);
    }
  }


  /**
   * @return entity id of this trust mark issuer.
   */
  public EntityID getEntityId() {
    return this.trustMarkIssuerProperties.entityIdentifier();
  }
}
