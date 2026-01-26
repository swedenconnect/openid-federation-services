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

package se.swedenconnect.oidf.resolver.trustmark;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;
import java.util.Date;

public class TrustMarkFactory {
  public static String createTrustMark(final String issuer, final String subject, final JWK signKey) {
    try {
      final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
          .type(new JOSEObjectType("trust-mark+jwt"))
          .build();

      final JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
          .issuer(issuer)
          .subject(subject)
          .claim("trust_mark_type", subject)
          .claim("policy_uri", "http://openid.swedenconnect.se/policy")
          .issueTime(Date.from(Instant.now()))
          .build();

      final SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);
      signedJWT.sign(new RSASSASigner(signKey.toRSAKey()));
      return signedJWT.serialize();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
