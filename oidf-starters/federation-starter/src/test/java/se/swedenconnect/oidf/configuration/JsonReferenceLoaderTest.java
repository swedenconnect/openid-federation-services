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

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.keys.KeyProperty;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;

import java.util.List;
import java.util.Map;

/**
 * Tests for the error reporting of {@link JsonReferenceLoader}.
 */
class JsonReferenceLoaderTest {

  private JsonReferenceLoader loader;

  @BeforeEach
  void setUp() throws Exception {
    final KeyRegistry registry = new KeyRegistry();
    final KeyProperty key = new KeyProperty();
    key.setKey(new ECKeyGenerator(Curve.P_256).keyID("kid-1").generate());
    key.setAlias("sign-key");
    key.setMapping(List.of("hosted"));
    registry.register(key);
    final ObjectProvider<KeyRegistry> provider =
        new StaticListableBeanFactory(Map.of("keyRegistry", registry)).getBeanProvider(KeyRegistry.class);
    final JWKPropertyLoader jwkPropertyLoader = new JWKPropertyLoader(provider);
    this.loader = new JsonReferenceLoader(jwkPropertyLoader, new JWKSPropertyLoader(jwkPropertyLoader));
  }

  @Test
  void misspelledKeyReferenceNamesFileEntryAndReference() {
    final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.loader.loadJson("classpath:misspelled-key-trust-anchors.json",
            new TypeToken<List<TrustAnchorProperties>>() {
            }));

    Assertions.assertEquals("Failed to load classpath:misspelled-key-trust-anchors.json at "
        + "$[0].subordinates[1].jwks (entry https://example.com/rp): "
        + "Key reference 'hosted:sing-key' could not be found", e.getMessage());
  }

  @Test
  void locationWithoutEntityIdentifierIsThePath() {
    Assertions.assertEquals("$.a[1]", JsonReferenceLoader.describeLocation("{\"a\": [1, 2]}", "$.a[1]"));
  }
}
