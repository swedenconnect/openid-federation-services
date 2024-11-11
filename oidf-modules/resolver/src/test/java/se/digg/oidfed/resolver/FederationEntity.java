package se.digg.oidfed.resolver;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class FederationEntity {
  private final String issuer;
  private final String subject;
  private final JWK signKey;
  private final JWK selfKey;
  private final List<Consumer<JWTClaimsSet.Builder>> customizers;

  public FederationEntity(final String issuer, final String subject, final JWK signKey, final JWK selfKey,
      final List<Consumer<JWTClaimsSet.Builder>> customizers) {
    this.issuer = issuer;
    this.subject = subject;
    this.signKey = signKey;
    this.selfKey = selfKey;
    this.customizers = customizers;
  }

  public String getIssuer() {
    return issuer;
  }

  public String getSubject() {
    return subject;
  }

  public String getLocation() {
    if (issuer.equalsIgnoreCase(subject)) {
      return "%s/.well-known/openid-federation".formatted(subject);
    }
    return "%s/fetch?sub=%s".formatted(issuer, URLEncoder.encode(subject, Charset.defaultCharset()));
  }

  public String getListLocation() {
    if (issuer.equalsIgnoreCase(subject)) {
      return "%s/list".formatted(subject);
    }
    return "";
  }

  public JWK getSelfKey() {
    return selfKey;
  }

  public JWK getSignKey() {
    return signKey;
  }

  public String getSignedJwt() {
    try {
      final Instant now = Instant.now();

      final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
          .issuer(issuer)
          .subject(subject)
          .issueTime(Date.from(now))
          .expirationTime(Date.from(now.plus(1, ChronoUnit.DAYS)))
          .claim("jwks", Map.of("keys", List.of(selfKey.toPublicJWK().toJSONObject())));

      customizers.forEach(c -> c.accept(builder));

      final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
          .type(new JOSEObjectType("entity-statement+jwt"))
          .build();

      final SignedJWT signedJWT = new SignedJWT(header, builder.build());
      signedJWT.sign(new RSASSASigner(signKey.toRSAKey()));
      return signedJWT.serialize();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static class Builder {
    private String issuer;
    private String subject;
    private JWK selfKey;
    private JWK signKey;
    private final List<Consumer<JWTClaimsSet.Builder>> claimsCustomizers = new ArrayList<>();

    public Builder issuer(final String issuer) {
      this.issuer = issuer;
      return this;
    }

    public Builder subject(final String subject) {
      this.subject = subject;
      return this;
    }

    public Builder trustedKey(final JWK selfKey) {
      this.selfKey = selfKey;
      return this;
    }

    public Builder signKey(final JWK signKey) {
      this.signKey = signKey;
      return this;
    }

    public Builder customize(final Consumer<JWTClaimsSet.Builder> consumer) {
      this.claimsCustomizers.add(consumer);
      return this;
    }

    public FederationEntity build() {
      if (Objects.isNull(this.signKey)) {
        try {
          this.signKey = generateKey();
        }
        catch (JOSEException e) {
          throw new RuntimeException(e);
        }
      }

      if (this.issuer.equalsIgnoreCase(this.subject)) {
        this.selfKey = this.signKey;
      }
      else {
        if (Objects.isNull(this.selfKey)) {
          try {
            this.selfKey = generateKey();
          }
          catch (JOSEException e) {
            throw new RuntimeException(e);
          }
        }
      }
      return new FederationEntity(this.issuer, this.subject, this.signKey, this.selfKey, this.claimsCustomizers);
    }

    private static RSAKey generateKey() throws JOSEException {
      return new RSAKeyGenerator(2048)
          .keyUse(KeyUse.SIGNATURE)
          .keyID(UUID.randomUUID().toString())
          .issueTime(new Date())
          .generate();
    }
  }
}