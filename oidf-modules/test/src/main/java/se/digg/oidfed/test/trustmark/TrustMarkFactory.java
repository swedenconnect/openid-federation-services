package se.digg.oidfed.test.trustmark;

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
          .claim("id", subject)
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
