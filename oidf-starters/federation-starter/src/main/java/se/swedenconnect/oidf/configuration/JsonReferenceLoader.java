/*
 * Copyright 2024-2026 Sweden Connect
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
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.nimbusds.jose.shaded.gson.JsonPrimitive;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import com.nimbusds.jose.shaded.gson.stream.JsonReader;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import se.swedenconnect.oidf.common.entity.entity.integration.DurationDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.InstantDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSSerializer;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.JWKSerializer;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private static final List<String> SUPPORTED_REFERENCES = List.of("classpath:", "file:");

  /** Matches one segment of a JSON path, either {@code .name} or {@code [index]}. */
  private static final Pattern PATH_SEGMENT = Pattern.compile("\\.([^.\\[]+)|\\[(\\d+)]");

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
   * @return object
   * @param <T> type
   */
  public <T> T loadJson(final String source, final TypeToken<T> typeToken) {
    if (SUPPORTED_REFERENCES.stream().anyMatch(source::startsWith)) {
      final String json;
      try {
        json = getJson(source);
      } catch (final IOException e) {
        throw new IllegalArgumentException("Failed to read %s: %s".formatted(source, e.getMessage()), e);
      }
      final JsonReader reader = new JsonReader(new StringReader(json));
      try {
        return new GsonBuilder()
            .registerTypeAdapter(JWK.class, new JWKSerializer())
            .registerTypeAdapter(JWKSet.class,
                new JWKSSerializer(new ReferenceJWKSSerializer(this.jwksPropertyLoader), null))
            .registerTypeAdapter(Duration.class, new DurationDeserializer())
            .registerTypeAdapter(Instant.class, new InstantDeserializer())
            .registerTypeAdapter(EntityID.class, new EntityIdentifierDeserializer())
            .registerTypeAdapter(TrustMarkType.class, new TrustMarkIdentifierDeserializer())
            .addSerializationExclusionStrategy(EXCLUSION_STRATEGY)
            .addDeserializationExclusionStrategy(EXCLUSION_STRATEGY)
            .create()
            .getAdapter(typeToken)
            .read(reader);
      } catch (final IOException | RuntimeException e) {
        // Not set as cause, since Spring Boot only reports the root cause when binding fails
        final IllegalArgumentException failure = new IllegalArgumentException("Failed to load %s at %s: %s"
            .formatted(source, describeLocation(json, reader.getPath()), e.getMessage()));
        failure.addSuppressed(e);
        throw failure;
      }
    }
    throw new IllegalArgumentException("Could not determine metadata reference for %s".formatted(source));
  }

  /**
   * Describes where in a JSON document loading failed. The description holds the JSON path, followed by the entity
   * identifier of the closest enclosing entry that has one.
   *
   * @param json the document
   * @param path the JSON path where loading failed, for example {@code $[0].subordinates[1].jwks}
   * @return the description
   */
  static String describeLocation(final String json, final String path) {
    String entityId = null;
    try {
      JsonElement element = JsonParser.parseString(json);
      final Matcher matcher = PATH_SEGMENT.matcher(path);
      while (element != null && matcher.find()) {
        if (element.isJsonObject() && element.getAsJsonObject().get("entity-identifier") instanceof
            final JsonPrimitive id) {
          entityId = id.getAsString();
        }
        if (matcher.group(1) != null && element.isJsonObject()) {
          element = element.getAsJsonObject().get(matcher.group(1));
        }
        else if (matcher.group(2) != null && element.isJsonArray()
            && Integer.parseInt(matcher.group(2)) < element.getAsJsonArray().size()) {
          element = element.getAsJsonArray().get(Integer.parseInt(matcher.group(2)));
        }
        else {
          element = null;
        }
      }
    }
    catch (final RuntimeException e) {
      // The location is only used in an error message, so an unparsable document gives the path alone
    }
    return entityId == null ? path : "%s (entry %s)".formatted(path, entityId);
  }

  private static @NonNull String getJson(final String source) throws IOException {
    if (source.startsWith("classpath:")) {
      return new ClassPathResource(source.split("classpath:")[1])
          .getContentAsString(StandardCharsets.UTF_8);
    }
    if (source.startsWith("file:")) {
      return new FileSystemResource(source.split("file:")[1])
          .getContentAsString(StandardCharsets.UTF_8);
    }
    throw new IllegalArgumentException("Could not determine metadata reference for %s".formatted(source));
  }
}
