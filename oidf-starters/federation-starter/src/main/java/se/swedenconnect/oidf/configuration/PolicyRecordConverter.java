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

import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.PolicyRecord;

import java.util.Map;

/**
 * Converter for loading policy records.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class PolicyRecordConverter implements Converter<String, PolicyRecord> {
  private final JsonReferenceLoader jsonReferenceLoader;
  @Nullable
  @Override
  public PolicyRecord convert(final String source) {
    final Map<String, Object> policy = this.jsonReferenceLoader.loadJson(source);
    return new PolicyRecord(source, policy);
  }
}
