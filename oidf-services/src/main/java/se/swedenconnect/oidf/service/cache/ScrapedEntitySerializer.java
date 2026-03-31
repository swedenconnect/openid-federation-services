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
package se.swedenconnect.oidf.service.cache;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.JsonDeserializationContext;
import com.nimbusds.jose.shaded.gson.JsonDeserializer;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonParseException;
import com.nimbusds.jose.shaded.gson.JsonPrimitive;
import com.nimbusds.jose.shaded.gson.JsonSerializationContext;
import com.nimbusds.jose.shaded.gson.JsonSerializer;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.InstantDeserializer;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Implementation of {@link RedisSerializer} for {@link ScrapedEntity} using GSON.
 *
 * @author Felix Hellman
 */
public class ScrapedEntitySerializer implements RedisSerializer<ScrapedEntity> {

  private final Gson gson;

  /**
   * Constructor.
   */
  public ScrapedEntitySerializer() {
    this.gson = new GsonBuilder()
        .registerTypeAdapter(EntityID.class, new EntityIdentifierDeserializer())
        .registerTypeAdapter(Instant.class, new InstantDeserializer())
        .registerTypeAdapter(EntityStatement.class, new EntityStatementAdapter())
        .registerTypeAdapter(SignedJWT.class, new SignedJWTAdapter())
        .create();
  }

  @Override
  public byte[] serialize(final ScrapedEntity value) throws SerializationException {
    if (value == null) {
      return null;
    }
    return this.gson.toJson(value).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public ScrapedEntity deserialize(final byte[] bytes) throws SerializationException {
    if (bytes == null) {
      return null;
    }
    return this.gson.fromJson(new String(bytes, StandardCharsets.UTF_8), ScrapedEntity.class);
  }

  private static class EntityStatementAdapter
      implements JsonSerializer<EntityStatement>, JsonDeserializer<EntityStatement> {

    @Override
    public JsonElement serialize(
        final EntityStatement src, final Type typeOfSrc, final JsonSerializationContext context) {
      return new JsonPrimitive(src.getSignedStatement().serialize());
    }

    @Override
    public EntityStatement deserialize(
        final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
        throws JsonParseException {
      try {
        return EntityStatement.parse(json.getAsString());
      }
      catch (final ParseException e) {
        throw new JsonParseException(e);
      }
    }
  }

  private static class SignedJWTAdapter
      implements JsonSerializer<SignedJWT>, JsonDeserializer<SignedJWT> {

    @Override
    public JsonElement serialize(
        final SignedJWT src, final Type typeOfSrc, final JsonSerializationContext context) {
      return new JsonPrimitive(src.serialize());
    }

    @Override
    public SignedJWT deserialize(
        final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
        throws JsonParseException {
      try {
        return SignedJWT.parse(json.getAsString());
      }
      catch (final java.text.ParseException e) {
        throw new JsonParseException(e);
      }
    }
  }
}
