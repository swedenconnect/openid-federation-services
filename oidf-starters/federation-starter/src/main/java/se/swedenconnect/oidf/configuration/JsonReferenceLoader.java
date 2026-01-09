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
package se.swedenconnect.oidf.configuration;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import se.swedenconnect.oidf.common.entity.entity.integration.DurationDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.InstantDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSSerializer;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkId;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.JWKSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord.EXCLUSION_STRATEGY;

/**
 * Class to load json data by a string reference.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class JsonReferenceLoader {

  private final JWKPropertyLoader jwkPropertyLoader;
  private final JWKSPropertyLoader jwksPropertyLoader;

  /**
   * Load json from reference
   * @param source to load from
   * @return json
   */
  public Map<String, Object> loadJson(final String source) {
    return this.loadJson(source, new TypeToken<>() {
    });
  }

  /**
   * Load json from reference
   * @param source to load from
   * @param typeToken to load
   * @return object <T>
   * @param <T> type
   */
  public <T> T loadJson(final String source, final TypeToken<T> typeToken) {
    if (source.startsWith("classpath:")) {
      final String json;
      try {
        json = new ClassPathResource(source.split("classpath:")[1])
            .getContentAsString(StandardCharsets.UTF_8);
        return new GsonBuilder()
            .registerTypeAdapter(JWK.class, new JWKSerializer())
            .registerTypeAdapter(JWKSet.class,
                new JWKSSerializer(new ReferenceJWKSSerializer(this.jwksPropertyLoader), null))
            .registerTypeAdapter(Duration.class, new DurationDeserializer())
            .registerTypeAdapter(Instant.class, new InstantDeserializer())
            .registerTypeAdapter(EntityID.class, new EntityIdentifierDeserializer())
            .registerTypeAdapter(TrustMarkId.class, new TrustMarkIdentifierDeserializer())
            .addSerializationExclusionStrategy(EXCLUSION_STRATEGY)
            .addDeserializationExclusionStrategy(EXCLUSION_STRATEGY)
            .create()
            .getAdapter(typeToken)
            .fromJson(json);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
    throw new IllegalArgumentException("Could not determine metadata reference for %s".formatted(source));
  }
}
