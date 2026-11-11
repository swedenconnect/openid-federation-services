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

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonSerializationContext;
import com.nimbusds.jose.shaded.gson.JsonSerializer;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import se.swedenconnect.oidf.common.entity.entity.integration.DurationDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
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
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.PolicyRecord;
import se.swedenconnect.oidf.common.entity.keys.KeyProperty;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests that a metadata policy operator with a JSON null value survives reading and caching of registry data.
 */
class PolicyNullValueTest {

  private static final String MODULE_JSON = """
      {"trust-anchors": [{"entity-identifier": "https://example.com/ta",
        "subordinates": [{"entity-identifier": "https://example.com/im", "jwks": "hosted:sign-key-1",
          "policy": {"id": "policy-1", "policy": {"openid_relying_party": {
            "signed_jwks_uri": {"value": null},
            "id_token_signed_response_alg": {"value": null, "essential": false},
            "contacts": {"add": ["a@example.com", null]},
            "grant_types": {"subset_of": ["authorization_code"]}
          }}}}]}]}
      """;

  private Gson gson;

  /** Gson configured as before null values in policies were kept. */
  private Gson legacyGson;

  @BeforeEach
  void setUp() throws Exception {
    final KeyRegistry registry = new KeyRegistry();
    final KeyProperty property = new KeyProperty();
    property.setKey(new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).keyID("k1").generate());
    property.setAlias("sign-key-1");
    property.setMapping(List.of("hosted"));
    registry.register(property);
    final JWKSKidReferenceLoader loader = new JWKSKidReferenceLoader(registry);
    this.gson = new FederationRegistryConfiguration().registryGson(loader, registry);
    this.legacyGson = new GsonBuilder()
        .registerTypeAdapter(Duration.class, new DurationDeserializer())
        .registerTypeAdapter(Instant.class, new InstantDeserializer())
        .registerTypeAdapter(EntityID.class, new EntityIdentifierDeserializer())
        .registerTypeAdapter(TrustMarkType.class, new TrustMarkIdentifierDeserializer())
        .registerTypeAdapter(JWKSet.class, new JWKSSerializer(loader, loader))
        .registerTypeAdapter(CompositeRecord.class, new LegacyCompositeRecordSerializer())
        .registerTypeAdapter(EntityRecord.class, new EntityRecordDeserializer(loader, registry))
        .create();
  }

  @Test
  void readingRegistryJsonKeepsNullOperatorValue() {
    final ModuleRecord module = new JsonRegistryLoader(this.gson).parseModuleJson(MODULE_JSON);

    assertNullOperatorValuesPresent(module);
  }

  @Test
  void cacheRoundTripKeepsNullOperatorValue() {
    final ModuleRecord module = new JsonRegistryLoader(this.gson).parseModuleJson(MODULE_JSON);
    final CompositeRecord record = this.compositeRecord(List.of(), module);

    // The same calls as RedisCompositeRecordSerializer
    final String json = this.gson.toJson(record);
    final CompositeRecord restored = this.gson.fromJson(json, CompositeRecord.class);

    assertNullOperatorValuesPresent(restored.getModuleRecord().getValue());
  }

  @Test
  void cachedJsonHasNoNullsOutsidePolicy() {
    final ModuleRecord module = new JsonRegistryLoader(this.gson).parseModuleJson(MODULE_JSON);

    final String json = this.gson.toJson(this.compositeRecord(List.of(), module));

    Assertions.assertTrue(json.contains("\"signed_jwks_uri\":{\"value\":null}"), json);
    Assertions.assertFalse(json.contains("virtual-entity-id"), json);
    Assertions.assertFalse(json.contains("ec-location"), json);
    Assertions.assertEquals(3, json.split("null", -1).length - 1, json);
  }

  @Test
  void policyRecordWithoutIdOrPolicyIsWrittenAsBefore() {
    Assertions.assertEquals("{}", this.gson.toJson(new PolicyRecord(null, null)));
    Assertions.assertEquals("{\"id\":\"p\"}", this.gson.toJson(new PolicyRecord("p", null)));
    Assertions.assertEquals("{\"policy\":{\"openid_provider\":{}}}",
        this.gson.toJson(new PolicyRecord(null, Map.of("openid_provider", Map.of()))));
  }

  @Test
  void cachedJsonWithoutNullsIsUnchanged() throws Exception {
    final JsonRegistryLoader loader = new JsonRegistryLoader(this.gson);
    final CompositeRecord record = this.compositeRecord(
        loader.parseEntityRecord(new ClassPathResource("testentities.json").getContentAsString(StandardCharsets.UTF_8)),
        loader.parseModuleJson(new ClassPathResource("modules.json").getContentAsString(StandardCharsets.UTF_8)));

    // The state hash and the Redis cache both use this serialization
    Assertions.assertEquals(this.legacyGson.toJson(record), this.gson.toJson(record));
  }

  private CompositeRecord compositeRecord(final List<EntityRecord> entities, final ModuleRecord module) {
    final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    return new CompositeRecord(
        new Expirable<>(now.plusSeconds(60), now, entities),
        new Expirable<>(now.plusSeconds(60), now, module));
  }

  @SuppressWarnings("unchecked")
  private static void assertNullOperatorValuesPresent(final ModuleRecord module) {
    final Map<String, Object> policy = module.getTrustAnchors().getFirst().getSubordinates().getFirst()
        .getPolicy().getPolicy();
    final Map<String, Object> rp = (Map<String, Object>) policy.get("openid_relying_party");

    final Map<String, Object> signedJwksUri = (Map<String, Object>) rp.get("signed_jwks_uri");
    Assertions.assertTrue(signedJwksUri.containsKey("value"));
    Assertions.assertNull(signedJwksUri.get("value"));

    final Map<String, Object> idTokenAlg = (Map<String, Object>) rp.get("id_token_signed_response_alg");
    Assertions.assertTrue(idTokenAlg.containsKey("value"));
    Assertions.assertNull(idTokenAlg.get("value"));
    Assertions.assertEquals(false, idTokenAlg.get("essential"));

    Assertions.assertEquals(Map.of("add", Arrays.asList("a@example.com", null)), rp.get("contacts"));
    Assertions.assertEquals(Map.of("subset_of", List.of("authorization_code")), rp.get("grant_types"));
  }

  /**
   * The tree based composite record serializer used before null values in policies were kept.
   */
  private static class LegacyCompositeRecordSerializer implements JsonSerializer<CompositeRecord> {

    @Override
    public JsonElement serialize(
        final CompositeRecord compositeRecord, final Type type, final JsonSerializationContext context) {
      final JsonObject json = new JsonObject();
      json.add("expiration", context.serialize(compositeRecord.getExpiration()));
      json.add("issuedAt", context.serialize(compositeRecord.getIssuedAt()));
      json.add("entities", context.serialize(compositeRecord.getEntityRecords().getValue()));
      json.add("module", context.serialize(compositeRecord.getModuleRecord().getValue()));
      return json;
    }
  }
}
