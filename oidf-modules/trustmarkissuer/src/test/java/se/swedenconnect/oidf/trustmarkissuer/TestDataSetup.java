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
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkClaimsSet;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkDelegation;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkId;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectRecord;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Data setup for testing
 *
 * @author Per Fredrik Plars
 */
public class TestDataSetup {

  public static TrustMarkIssuerProperties trustMarkProperties() throws JOSEException {

    final TrustMarkIssuerProperties trustMarkIssuerProperties = TrustMarkIssuerProperties.builder()
        .trustMarkValidityDuration(Duration.of(5, ChronoUnit.MINUTES))
        .issuerEntityId(new EntityID("https://tmissuer.digg.se"))
        .trustMarks(new ArrayList<>())
        .build();

    final TrustMarkSubjectRecord sub1 =
        TrustMarkSubjectRecord.builder()
            .sub("http://tm1.digg.se/sub1")
            .expires(Instant.now().plus(10, ChronoUnit.MINUTES))
            .granted(Instant.now())
            .build();

    trustMarkIssuerProperties.trustMarks()
        .add(TrustMarkProperties.builder()
            .trustMarkId(TrustMarkId.create("http://tm.digg.se/default"))
            .logoUri(Optional.empty())
            .refUri(Optional.empty())
            .delegation(Optional.empty())
            .build());

    return trustMarkIssuerProperties;
  }

  public static SignedJWT createTrustMarkDelegation() throws JOSEException, ParseException {

    final RSAKey rsaKey = new RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .issueTime(new Date())
        .generate();

    final JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .keyID(rsaKey.getKeyID())
        .type(new JOSEObjectType(TrustMarkDelegation.DELEGATION_TYPE))
        .build();

    final TrustMarkClaimsSet trustMarkClaimsSet = new TrustMarkClaimsSet(
        Issuer.parse("http://issuer.digg.se"),
        new Subject("http://subject.digg.se"),
        new Identifier(UUID.randomUUID().toString()),
        new Date());

    final SignedJWT trustMarkDelegate = new SignedJWT(jwsHeader, trustMarkClaimsSet.toJWTClaimsSet());
    trustMarkDelegate.sign(new RSASSASigner(rsaKey));
    return trustMarkDelegate;
  }

  private static RSAKey generateKey() {

    final RSAKey rsaKey;
    try {
      rsaKey = new RSAKeyGenerator(2048)
          .keyUse(KeyUse.SIGNATURE)
          .keyID(UUID.randomUUID().toString())
          .issueTime(new Date())
          .generate();
    }
    catch (JOSEException e) {
      throw new RuntimeException(e);
    }
    return rsaKey;
  }


}
