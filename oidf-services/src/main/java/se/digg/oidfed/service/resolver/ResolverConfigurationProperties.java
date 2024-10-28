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
package se.digg.oidfed.service.resolver;

import com.nimbusds.jose.jwk.JWK;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import se.digg.oidfed.resolver.ResolverProperties;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

/**
 * Configuration properties for Resolver.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@ConfigurationProperties(ResolverConfigurationProperties.PROPERTY_PATH)
public class ResolverConfigurationProperties {

  /** Property path */
  public static final String PROPERTY_PATH = "openid.federation.resolver";

  /** Set to true if this module should be active or not. */
  private Boolean active;

  /** Supported trustAnchor for this resolver*/
  private String trustAnchor;

  /**Duration for resolve responses*/
  private Duration duration = Duration.of(7, ChronoUnit.DAYS);

  private List<String> trustedJwks;

  private String signKey;

  private String entityIdentifier;

  private String trustStoreBundle;

  /**
   * @return properties in non spring specific specific format.
   */
  public ResolverProperties toResolverProperties() {
    return new ResolverProperties(trustAnchor, duration, parseTrustedJwks(), entityIdentifier, signKey());
  }

  /**
   * @return list of jwks parsed from configuration
   */
  public List<JWK> parseTrustedJwks() {
    return trustedJwks.stream().map(s -> {
      try {
        return parseKey(s);
      }
      catch (ParseException e) {
        throw new RuntimeException(e);
      }
    }).toList();
  }

  /**
   * @return sign key parsed from configuration
   */
  public JWK signKey() {
    try {
      return ResolverConfigurationProperties.parseKey(signKey);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private static JWK parseKey(final String s) throws ParseException {
    return JWK.parse(new String(Base64.getDecoder().decode(s), Charset.defaultCharset()));
  }
}
