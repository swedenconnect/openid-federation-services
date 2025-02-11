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
package se.digg.oidfed.service.cache;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import se.digg.oidfed.common.entity.integration.Expirable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link RedisSerializer} for {@link EntityStatement}
 *
 * @author Felix Hellman
 */
public class ExpirableEntityStatementSerializer implements RedisSerializer<Expirable<EntityStatement>> {

  private final RedisSerializer<EntityStatement> entityStatementSerializer = new EntityStatementSerializer();

  @Override
  public byte[] serialize(final Expirable<EntityStatement> value) throws SerializationException {
    if (value == null) {
      return null;
    }
    //Serialize signed statement to JWT
    final String data = value.getValue().getSignedStatement().serialize();
    return data.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public Expirable<EntityStatement> deserialize(final byte[] bytes) throws SerializationException {
    if (bytes == null) {
      return null;
    }
    try {
      final EntityStatement entityStatement = EntityStatement.parse(new String(bytes, Charset.defaultCharset()));
      return new Expirable<>(
          entityStatement.getClaimsSet().getExpirationTime().toInstant(),
          entityStatement
      );
    }
    catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
