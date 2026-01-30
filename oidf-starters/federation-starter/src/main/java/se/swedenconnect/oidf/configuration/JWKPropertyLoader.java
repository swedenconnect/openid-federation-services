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

import com.nimbusds.jose.jwk.JWK;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;

/**
 * Converter to load JWK from string.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class JWKPropertyLoader implements Converter<String, JWK> {

  private final ObjectProvider<KeyRegistry> registry;

  @Nullable
  @Override
  public JWK convert(final String source) {
    if (source.startsWith("federation:")
        || source.startsWith("hosted:")
        || source.startsWith("public")) {
      return this.registry.getObject().getKey(source)
          .orElseThrow();
    }
    throw new IllegalArgumentException("Could not convert string:%s to jwk".formatted(source));
  }
}
