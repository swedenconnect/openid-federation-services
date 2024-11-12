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
package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkClaimsSet;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.trustmarkissuer.configuration.TrustMarkIssuerProperties;
import se.digg.oidfed.trustmarkissuer.configuration.TrustMarkProperties;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;
import se.digg.oidfed.trustmarkissuer.exception.InvalidRequestException;
import se.digg.oidfed.trustmarkissuer.exception.NotFoundException;
import se.digg.oidfed.trustmarkissuer.exception.ServerErrorException;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static se.digg.oidfed.trustmarkissuer.validation.FederationAssert.assertNotEmpty;
import static se.digg.oidfed.trustmarkissuer.validation.FederationAssert.assertNotEmptyThrows;
import static se.digg.oidfed.trustmarkissuer.validation.FederationAssert.throwIfNull;

/**
 * Implementation of Trust Mark Issuer.
 *
 * @author Felix Hellman
 */
@Slf4j
public class TrustMarkIssuer {

  public static final JOSEObjectType TRUST_MARK_JWT_TYPE = new JOSEObjectType("trust-mark+jwt");
  private static final SecureRandom rng = new SecureRandom();

  private final TrustMarkProperties trustMarkProperties;

  /**
   * TrustMarkIssuer
   * @param trustMarkProperties ConfigurationProperties
   */
  public TrustMarkIssuer(final TrustMarkProperties trustMarkProperties) {
    this.trustMarkProperties = assertNotEmpty(trustMarkProperties, "TrustMarkProperties can not be null");
    this.trustMarkProperties.validate();
  }

  /**
   * Listing al trustmarks that are valid for this trustmarkid filtered by subject if supplied
   *
   * @param request Request containing trustmarkid and subject
   * @return listing of trust mark subjects that are valid. If there is no subject found a empty list is returned.
   */
  public List<String> trustMarkListing(final TrustMarkListingRequest request)
      throws InvalidRequestException, NotFoundException {
    assertNotEmptyThrows(request, () -> new InvalidRequestException("TrustMarkListingRequest is expected"));

    return getTrustMarkSubject(TrustMarkId.validate(request.trustMarkId(), InvalidRequestException::new),
        request.subject())
        .map(TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties::getSub)
        .toList();
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
    return getTrustMarkSubject(TrustMarkId.create(request.trustMarkId()), request.subject()).findAny().isPresent();
  }

  /**
   * Creating a SignedTrustMark for this subject. https://openid.net/specs/openid-federation-1_0.html#section-8.6.1
   *
   * @param request TrustMarkId and Subject is mandatory
   * @return trust mark in a JWT
   */
  public String trustMark(final TrustMarkRequest request)
      throws InvalidRequestException, NotFoundException, ServerErrorException {
    assertNotEmptyThrows(request.subject(), () -> new InvalidRequestException("Subject is expected"));
    final TrustMarkId trustMarkId = TrustMarkId.create(request.trustMarkId());

    final TrustMarkIssuerProperties trustMarkIssuerProperties = getTrustMarkIssuerProperties(trustMarkId)
        .orElseThrow(() -> new NotFoundException("TrustMark not found for id:'"+trustMarkId+"'"));

    final TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties trustMarkSubject =
        getTrustMarkSubject(trustMarkId, request.subject())
            .findFirst()
            .orElseThrow(() -> new NotFoundException(
                "TrustMark can not be found for TrustMarkId:'" + request.trustMarkId() + "' and subject:'"
                    + request.subject() + "'"));

    // https://openid.net/specs/openid-federation-1_0.html#name-trust-mark-claims
    final JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder()
        .issueTime(new Date(now().toEpochMilli()))
        .jwtID(new BigInteger(128, rng).toString(16))
        .subject(trustMarkSubject.getSub());

    claimsSetBuilder.expirationTime(
        calculateExp(trustMarkProperties.getTrustMarkValidityDuration(), trustMarkSubject.getExpires()));

    throwIfNull(this.trustMarkProperties.getIssuerEntityId(), claimsSetBuilder::issuer,
        () -> new ServerErrorException("Issuer must be present"));

    Optional.ofNullable(request.trustMarkId()).ifPresent( (value) -> claimsSetBuilder.claim("id", value));

    Optional.ofNullable(trustMarkIssuerProperties.getLogoUri())
        .ifPresent( (value) -> claimsSetBuilder.claim("logo_uri", value));

    Optional.ofNullable(trustMarkIssuerProperties.getRefUri())
        .ifPresent( (value) -> claimsSetBuilder.claim("ref", value));

    Optional.ofNullable(trustMarkIssuerProperties.getDelegation())
        .ifPresent( (value) -> claimsSetBuilder.claim("delegation", value));

    final JWTClaimsSet claimsSet = claimsSetBuilder.build();

    final JWK jwk = assertNotEmptyThrows(trustMarkProperties.getSignJWK(),
        () -> new ServerErrorException("Unable to find key to sign JWK TrustMark"));
    assertNotEmptyThrows(jwk.getKeyID(), () -> new ServerErrorException("Kid is expected on sign key"));

    final JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .keyID(jwk.getKeyID())
        .type(TRUST_MARK_JWT_TYPE)
        .build();
    try {
      final TrustMarkClaimsSet trustMarkClaimsSet = new TrustMarkClaimsSet(claimsSet);
      final SignedJWT signedTrustMarkJWT = new SignedJWT(jwsHeader, trustMarkClaimsSet.toJWTClaimsSet());

      signedTrustMarkJWT.sign(new RSASSASigner(jwk.toRSAKey()));
      return signedTrustMarkJWT.serialize();
    }
    catch (JOSEException | ParseException e) {
      throw new ServerErrorException("Unable to sign TrustMarkId: '" + request.trustMarkId() +
          "' Subject:'"+request.subject()+"'", e);
    }

  }

  /**
   * Calculate exp value. If current time plus trustmarkTimeToLive is grater then subjectTimeToLive. SubjectTimeToLive
   * will be used.
   *
   * @param trustMarkDurationToLive Duration that the TrustMark JWT is expected to live.
   * @param subjectTimeToLive End time for when the subject is valid.
   * @return Date that represents the exp field in Trustmark JWT.
   */
  protected Date calculateExp(final Duration trustMarkDurationToLive,final Instant subjectTimeToLive) {
    final Instant calculatedTrustMarkTTL = now().plus(trustMarkDurationToLive);
    if (subjectTimeToLive != null && calculatedTrustMarkTTL.isAfter(subjectTimeToLive)) {
      return new Date(subjectTimeToLive.toEpochMilli());
    }
    return new Date(calculatedTrustMarkTTL.toEpochMilli());
  }

  /**
   * Finding a TrustMarkProperty from its TrausMarkId.
   * @param trustMarkId TrustMarkId to find
   * @return If TrustMarkIssuerProperties is found a Optional is returned.
   */
  private Optional<TrustMarkIssuerProperties> getTrustMarkIssuerProperties(TrustMarkId trustMarkId){
    assertNotEmpty(trustMarkId,"TrustMarkId is expected");
    return trustMarkProperties.getTrustMarks().stream()
        .filter(trustMarkIssuerProperties -> trustMarkId.equals(trustMarkIssuerProperties.getTrustMarkId()))
        .findFirst();
  }

  /**
   * Finding TrustMAkrSubject
   * @param trustMarkId Mandatory TrustMarkId
   * @param subject Subject is an optional search parameter
   * @return Stream with TrustMarkIssuerSubjectProperties
   * @throws InvalidRequestException If TrustMarkId is not supplied
   * @throws NotFoundException If a trustmark is not found
   */
  private Stream<TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties> getTrustMarkSubject(
      final TrustMarkId trustMarkId, final String subject) throws InvalidRequestException, NotFoundException {
    assertNotEmptyThrows(trustMarkId, () -> new InvalidRequestException("TrustMarkId is expected"));

    final TrustMarkIssuerProperties trustMarkIssuerProperties =
        this.getTrustMarkIssuerProperties(trustMarkId)
            .orElseThrow(() -> new NotFoundException(
                "TrustMark can not be found for trust_mark_id:'" + trustMarkId + "'"));

    return trustMarkIssuerProperties.getSubjects().stream()
        .filter(this::isTrustMarkValidInTime)
        .filter(subProp -> subject == null || subProp.getSub().equals(subject));
  }

  /**
   * Testing that this trustmark is valid. That it has not expired or that it is revoked
   *
   * @param trustMarkSubjectRecord Trustmark record
   * @return True if valid false otherwise
   */
  protected boolean isTrustMarkValidInTime(
      final TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties trustMarkSubjectRecord) {
    if(trustMarkSubjectRecord.isRevoked()){
      log.debug("Trust Mark is revoked. sub:'{}'", trustMarkSubjectRecord.getSub());
      return false;
    }

    final Instant expires = trustMarkSubjectRecord.getExpires();
    if (expires != null && now().isAfter(expires)) {
      log.debug("Trust Mark for sub:'{}' has expired", trustMarkSubjectRecord.getSub());
      return false;
    }
    final Instant granted = trustMarkSubjectRecord.getGranted();
    if (granted != null && now().isBefore(granted)) {
      log.debug("Trust Mark for sub:'{}' is not yet granted", trustMarkSubjectRecord.getSub());
      return false;
    }
    log.debug("Trust Mark for sub:'{}' is valid", trustMarkSubjectRecord.getSub());

    return true;
  }

  /**
   * Method for overloading and change time if needed
   *
   * @return LocalDate of now
   */
  protected Instant now() {
    return Instant.now();
  }

}
