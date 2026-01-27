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
package se.swedenconnect.oidf.service.entity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import se.swedenconnect.oidf.common.entity.entity.integration.JsonRegistryLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Signer for {@link EntityRecord}
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class RegistryRecordSigner {

  private final JWSSigner signer;
  private final JsonRegistryLoader jsonRegistryLoader;

  public SignedJWT signModules(final ModuleRecord response) throws JOSEException {
    return this.signJson("module_records", this.jsonRegistryLoader.toJson(response), "module-trustMarkSubjects" +
                                                                                     "+jwt");
  }

  /**
   * @param records to sign
   * @return a signed jwt
   * @throws JOSEException if signature fails
   */
  public SignedJWT signRecords(final List<EntityRecord> records) throws JOSEException {
    final String json = this.jsonRegistryLoader.toJson(records);
    return this.signJson("entity_records", json, "entity-trustMarkSubjects+jwt");
  }

  public @NotNull SignedJWT signJson(final String entity_records, final String json, final String type) throws JOSEException {
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .claim(entity_records, json)
        .expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .issueTime(Date.from(Instant.now()))
        .build();

    final JWSAlgorithm alg = this.signer.supportedJWSAlgorithms().stream().findFirst().get();
    final JWSHeader header = new JWSHeader.Builder(alg)
        .type(new JOSEObjectType(type))
        .build();

    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(this.signer);
    return jwt;
  }
}
