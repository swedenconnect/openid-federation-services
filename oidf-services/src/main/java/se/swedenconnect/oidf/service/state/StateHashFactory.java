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
package se.swedenconnect.oidf.service.state;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.jwk.JWKSet;
import org.bouncycastle.util.encoders.Hex;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Class responsible for hashing internal state.
 *
 * @author Felix Hellman
 */
public class StateHashFactory {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.registerModule(new JavaTimeModule());

    final SimpleModule module = new SimpleModule();
    module.addSerializer(JWKSet.class, new JWKSetSerializer());
    module.addDeserializer(JWKSet.class, new JWKSetDeserializer());
    MAPPER.registerModule(module);

  }

  /**
   * @param record to hash
   * @return hash of record
   * @throws Exception
   */
  public static String hashState(final CompositeRecord record) throws Exception {
    final String json = StateHashFactory.MAPPER.writerFor(CompositeRecord.class).writeValueAsString(record);
    final MessageDigest digest = MessageDigest.getInstance("SHA-256");
    final byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
    return new String(Hex.encode(hash));
  }

  /**
   * A deserializer for {@link JWKSet} instances used to convert JSON representations into {@link JWKSet} objects.
   */
  public static class JWKSetDeserializer extends JsonDeserializer<JWKSet> {
    @Override
    public JWKSet deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
      final Map<String, Object> jwkMap = p.readValueAs(new TypeReference<>() {});
      try {
        return JWKSet.parse(jwkMap);
      }
      catch (final Exception e) {
        throw new IOException("Failed to parse JWKSet", e);
      }
    }
  }

  /**
   * A custom serializer for {@link JWKSet} objects. This serializer ensures that the JSON representation
   * of the {@link JWKSet} maintains a predictable order of keys by recursively sorting the keys alphabetically.
   * This is achieved by processing the {@link JWKSet} into a {@link TreeMap} structure where applicable.
   */
  public static class JWKSetSerializer extends JsonSerializer<JWKSet> {
    private Object preserveOrderRecursive(final Object obj) {
      if (obj instanceof Map<?, ?> map) {
        final TreeMap<String, Object> treeMap = new TreeMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          final String key = entry.getKey().toString();
          final Object value = this.preserveOrderRecursive(entry.getValue()); // rekursivt
          treeMap.put(key, value);
        }
        return treeMap;
      }else if (obj instanceof List<?> list) {
        final List<Object> newList = new ArrayList<>();
        for (Object item : list) {
          newList.add(this.preserveOrderRecursive(item));
        }
        return newList;
      }
      else {
        return obj;
      }
    }

    @Override
    public void serialize(final JWKSet value,
        final JsonGenerator gen,
        final SerializerProvider serializers) throws IOException {

      if (value == null) {
        gen.writeNull();
        return;
      }
      gen.writeObject(this.preserveOrderRecursive(value.toJSONObject()));
    }
  }
}
