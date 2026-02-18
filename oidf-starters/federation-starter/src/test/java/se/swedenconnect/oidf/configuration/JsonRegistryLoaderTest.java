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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.shaded.gson.ExclusionStrategy;
import com.nimbusds.jose.shaded.gson.FieldAttributes;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import se.swedenconnect.oidf.common.entity.entity.integration.DurationDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.InstantDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSKidReferenceLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSSerializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JsonRegistryLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.keys.KeyProperty;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

class JsonRegistryLoaderTest {
  @Test
  void test() throws IOException {
    final KeyRegistry registry = new KeyRegistry();
    final KeyProperty property = new KeyProperty();
    property.setKey(generateKey());
    property.setAlias("sign-key-1");
    property.setMapping("hosted");
    registry.register(property);
    final JsonRegistryLoader jsonRegistryLoader =
        new JsonRegistryLoader(this.createGson(new JWKSKidReferenceLoader(registry)));
    final List<EntityRecord> entityRecords =
        jsonRegistryLoader.parseEntityRecord(new ClassPathResource("testentities.json").getContentAsString(StandardCharsets.UTF_8));
    System.out.println(entityRecords);
  }

  @Test
  void moduleTest() throws IOException {
    final JsonRegistryLoader jsonRegistryLoader = new JsonRegistryLoader(this.createGson(new JWKSKidReferenceLoader(new KeyRegistry())));
    final ModuleRecord moduleRecord = jsonRegistryLoader.parseModuleJson(new ClassPathResource("modules.json").getContentAsString(StandardCharsets.UTF_8));
    System.out.println(moduleRecord);
  }

  private static RSAKey generateKey() {

    final RSAKey rsaKey;
    try {
      rsaKey = new RSAKeyGenerator(2048)
          .keyUse(KeyUse.SIGNATURE)
          .keyID(UUID.randomUUID().toString())
          .issueTime(new Date())
          .generate();
    }
    catch (JOSEException e) {
      throw new RuntimeException(e);
    }
    return rsaKey;
  }

  private Gson createGson (final JWKSKidReferenceLoader loader) {
    return new GsonBuilder()
        .addDeserializationExclusionStrategy(new ExclusionStrategy() {
          @Override
          public boolean shouldSkipField(final FieldAttributes fieldAttributes) {
            return false;
          }

          @Override
          public boolean shouldSkipClass(final Class<?> aClass) {
            return false;
          }
        })
        .registerTypeAdapter(Duration.class, new DurationDeserializer())
        .registerTypeAdapter(Instant.class, new InstantDeserializer())
        .registerTypeAdapter(EntityID.class, new EntityIdentifierDeserializer())
        .registerTypeAdapter(TrustMarkType.class, new TrustMarkIdentifierDeserializer())
        .registerTypeAdapter(JWKSet.class, new JWKSSerializer(loader, loader))
        .registerTypeAdapter(CompositeRecord.class, new CompositeRecordSerializer())
        .create();
  }
}