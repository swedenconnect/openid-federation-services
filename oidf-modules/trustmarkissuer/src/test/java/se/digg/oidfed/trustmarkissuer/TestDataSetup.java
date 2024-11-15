/*
 *  Copyright 2024 Sweden Connect
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkClaimsSet;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkDelegation;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Data setup for testing
 *
 * @author Per Fredrik Plars
 */
public class TestDataSetup {

  public static TrustMarkProperties trustMarkProperties() throws JOSEException {

    final TrustMarkProperties trustMarkProperties = TrustMarkProperties.builder()
        .trustMarkValidityDuration(Duration.of(5, ChronoUnit.MINUTES))
        .issuerEntityId("https://tmissuer.digg.se")
        .signKey(generateKey())
        .trustMarks(new ArrayList<>())
        .alias("tm")
        .build();

    final TrustMarkIssuerSubject sub1 =
        TrustMarkIssuerSubject.builder()
            .sub("http://tm1.digg.se/sub1")
            .expires(Optional.of(Instant.now().plus(10, ChronoUnit.MINUTES)))
            .granted(Optional.of(Instant.now()))
            .build();

    final TrustMarkIssuerSubjectInMemLoader trustMarkIssuerSubjectLoader = new TrustMarkIssuerSubjectInMemLoader();
    trustMarkIssuerSubjectLoader.register(sub1);

    trustMarkProperties.trustMarks()
        .add(TrustMarkProperties.TrustMarkIssuerProperties.builder()
            .trustMarkId(TrustMarkId.create("http://tm.digg.se/default"))
            .logoUri(Optional.empty())
            .refUri(Optional.empty())
            .delegation(Optional.empty())
            .trustMarkIssuerSubjectLoader(trustMarkIssuerSubjectLoader)
            .build());

    return trustMarkProperties;
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

  private static RSAKey generateKey() throws JOSEException {
    final RSAKey rsaKey = new RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .issueTime(new Date())
        .generate();
    return rsaKey;
  }


}
