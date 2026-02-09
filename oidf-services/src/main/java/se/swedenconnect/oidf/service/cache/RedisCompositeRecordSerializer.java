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
package se.swedenconnect.oidf.service.cache;

import com.nimbusds.jose.shaded.gson.Gson;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Composite record serializer for redis using underlying gson serialization.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class RedisCompositeRecordSerializer implements RedisSerializer<Expirable<CompositeRecord>> {
  private final Gson gson;

  @Nullable
  @Override
  public byte[] serialize(@Nullable final Expirable<CompositeRecord> value) throws SerializationException {
    return this.gson.toJson(value.getValue()).getBytes(StandardCharsets.UTF_8);
  }

  @Nullable
  @Override
  public Expirable<CompositeRecord> deserialize(@Nullable final byte[] bytes) throws SerializationException {
    if (Objects.nonNull(bytes)) {
      final CompositeRecord compositeRecord = this.gson.fromJson(new String(bytes), CompositeRecord.class);
      return new Expirable<>(compositeRecord.getExpiration(), compositeRecord.getIssuedAt(), compositeRecord);
    }
    return null;
  }
}
