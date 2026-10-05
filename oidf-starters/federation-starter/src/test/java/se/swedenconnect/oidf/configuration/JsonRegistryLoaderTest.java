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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.shaded.gson.ExclusionStrategy;
import com.nimbusds.jose.shaded.gson.FieldAttributes;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.JsonParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import se.swedenconnect.oidf.common.entity.entity.integration.DurationDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.InstantDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSKidReferenceLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSSerializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JsonRegistryLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.EntityRecordDeserializer;
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
    property.setMapping(List.of("hosted"));
    registry.register(property);
    final JsonRegistryLoader jsonRegistryLoader =
        new JsonRegistryLoader(this.createGson(registry));
    final List<EntityRecord> entityRecords =
        jsonRegistryLoader.parseEntityRecord(new ClassPathResource("testentities.json").getContentAsString(StandardCharsets.UTF_8));
    System.out.println(entityRecords);
  }

  @Test
  void moduleTest() throws IOException {
    final JsonRegistryLoader jsonRegistryLoader = new JsonRegistryLoader(this.createGson(new KeyRegistry()));
    final ModuleRecord moduleRecord = jsonRegistryLoader.parseModuleJson(new ClassPathResource("modules.json").getContentAsString(StandardCharsets.UTF_8));
    System.out.println(moduleRecord);
  }

  @Test
  void entityWithMissingKeyIsLeftOut() {
    final List<EntityRecord> entities = new JsonRegistryLoader(this.createGson(this.registryWithHostedKey()))
        .parseEntityRecord("""
            [
              {"entity-identifier": "https://example.com/op", "jwks": "hosted:sign-key-1"},
              {"entity-identifier": "https://example.com/rp", "jwks": "hosted:sign-key-1,hosted:sing-key-1"}
            ]
            """);

    Assertions.assertEquals(List.of("https://example.com/op"),
        entities.stream().map(e -> e.getEntityIdentifier().getValue()).toList());
  }

  @Test
  void entityWithoutJwksUsesDefaultKey() {
    final KeyRegistry registry = this.registryWithHostedKey();
    final List<EntityRecord> entities = new JsonRegistryLoader(this.createGson(registry))
        .parseEntityRecord("""
            [{"entity-identifier": "https://example.com/op"}]
            """);

    Assertions.assertEquals(new JWKSet(registry.getDefaultKey().orElseThrow()).getKeys(),
        entities.getFirst().getJwks().getKeys());
  }

  @Test
  void entityWithEmptyKeyReferenceUsesDefaultKey() {
    final KeyRegistry registry = this.registryWithHostedKey();
    final List<EntityRecord> entities = new JsonRegistryLoader(this.createGson(registry))
        .parseEntityRecord("""
            [{"entity-identifier": "https://example.com/op", "jwks": ""}]
            """);

    Assertions.assertEquals(new JWKSet(registry.getDefaultKey().orElseThrow()).getKeys(),
        entities.getFirst().getJwks().getKeys());
  }

  @Test
  void entityWithoutJwksIsLeftOutWithoutDefaultKey() {
    final List<EntityRecord> entities = new JsonRegistryLoader(this.createGson(new KeyRegistry()))
        .parseEntityRecord("""
            [{"entity-identifier": "https://example.com/op"}]
            """);

    Assertions.assertEquals(List.of(), entities);
  }

  @Test
  void moduleWithMissingKeyIsRejected() {
    final JsonRegistryLoader loader = new JsonRegistryLoader(this.createGson(this.registryWithHostedKey()));

    final JsonParseException e = Assertions.assertThrows(JsonParseException.class, () -> loader.parseModuleJson("""
        {"trust-anchors": [{"entity-identifier": "https://example.com/ta",
          "subordinates": [{"entity-identifier": "https://example.com/op", "jwks": "hosted:sing-key-1"}]}]}
        """));
    Assertions.assertEquals("Key reference 'hosted:sing-key-1' could not be found", e.getMessage());
  }

  private KeyRegistry registryWithHostedKey() {
    final KeyRegistry registry = new KeyRegistry();
    final KeyProperty property = new KeyProperty();
    property.setKey(generateKey());
    property.setAlias("sign-key-1");
    property.setMapping(List.of("hosted"));
    registry.register(property);
    return registry;
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

  private Gson createGson (final KeyRegistry registry) {
    final JWKSKidReferenceLoader loader = new JWKSKidReferenceLoader(registry);
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
        .registerTypeAdapter(EntityRecord.class, new EntityRecordDeserializer(loader, registry))
        .create();
  }
}