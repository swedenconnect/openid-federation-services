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

import com.nimbusds.jwt.SignedJWT;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link RedisSerializer} for {@link SignedJWT}
 *
 * @author Felix Hellman
 */
public class ExpirableEntityStatementSerializer implements RedisSerializer<Expirable<SignedJWT>> {

  @Override
  public byte[] serialize(final Expirable<SignedJWT> value) throws SerializationException {
    if (value == null) {
      return null;
    }
    //Serialize signed statement to JWT
    final String data = value.getValue().serialize();
    return data.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public Expirable<SignedJWT> deserialize(final byte[] bytes) throws SerializationException {
    if (bytes == null) {
      return null;
    }
    try {
      final SignedJWT entityStatement = SignedJWT.parse(new String(bytes, Charset.defaultCharset()));
      final com.nimbusds.jwt.JWTClaimsSet claims = EntityStatementClaims.claims(entityStatement);
      return new Expirable<>(
          claims.getExpirationTime().toInstant(),
          claims.getIssueTime().toInstant(),
          entityStatement
      );
    }
    catch (final java.text.ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
