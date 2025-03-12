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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.ModuleRecord;
import se.digg.oidfed.common.entity.integration.registry.records.TrustMarkRecord;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Signer for {@link EntityRecord}
 *
 * @author Felix Hellman
 */
public class RegistryRecordSigner {

  private final JWSSigner signer;

  /**
   * Constructor.
   *
   * @param signer for signing jwt
   */
  public RegistryRecordSigner(final JWSSigner signer) {
    this.signer = signer;
  }

  public SignedJWT signModules(final ModuleRecord response) throws JOSEException {
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .claim("module_records", response.toJson())
        .expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .issueTime(Date.from(Instant.now()))
        .build();

    final JWSAlgorithm alg = this.signer.supportedJWSAlgorithms().stream().findFirst().get();
    final JWSHeader header = new JWSHeader.Builder(alg)
        .type(new JOSEObjectType("module-trustMarkSubjects+jwt"))
        .build();

    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(this.signer);
    return jwt;
  }

  /**
   * @param records to sign
   * @return a signed jwt
   * @throws JOSEException if signature fails
   */
  public SignedJWT signRecords(final List<EntityRecord> records) throws JOSEException {
    final List<Map<String, Object>> entityRecords = records.stream()
        .map(EntityRecord::toJson)
        .toList();
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .claim("entity_records", entityRecords)
        .expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .issueTime(Date.from(Instant.now()))
        .build();

    final JWSAlgorithm alg = this.signer.supportedJWSAlgorithms().stream().findFirst().get();
    final JWSHeader header = new JWSHeader.Builder(alg)
        .type(new JOSEObjectType("entity-trustMarkSubjects+jwt"))
        .build();

    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(this.signer);
    return jwt;
  }

  public SignedJWT signTrustMarks(final List<TrustMarkRecord> tms) throws JOSEException {
    final List<Map<String, Object>> trustMarks = tms.stream().map(TrustMarkRecord::toJson).toList();

    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .claim("trustmark_records", trustMarks)
        .expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .issueTime(Date.from(Instant.now()))
        .build();

    final JWSAlgorithm alg = this.signer.supportedJWSAlgorithms().stream().findFirst().get();
    final JWSHeader header = new JWSHeader.Builder(alg)
        .type(new JOSEObjectType("trust-mark-record+jwt"))
        .build();

    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(this.signer);
    return jwt;
  }
}
