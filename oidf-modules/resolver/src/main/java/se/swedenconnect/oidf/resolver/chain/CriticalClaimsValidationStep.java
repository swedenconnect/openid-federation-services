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
package se.swedenconnect.oidf.resolver.chain;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;

import java.util.HashSet;
import java.util.List;

/**
 * Validates critical claims of the chain.
 *
 * @author Felix Hellman
 */
public class CriticalClaimsValidationStep implements ChainValidationStep {

  private final List<String> supportedCriticalClaims =
      List.of("subject_entity_configuration_location");

  @Override
  public void validate(final List<EntityStatement> chain) {
    if (chain.stream()
        .map(es -> es.getClaimsSet().getStringListClaim("crit"))
        .filter(criticalClaims -> criticalClaims != null && !criticalClaims.isEmpty())
        .anyMatch(criticalClaims -> !new HashSet<>(this.supportedCriticalClaims).containsAll(criticalClaims))
    ) {
      throw new IllegalArgumentException("Unsupported critical claims declaration in Entity Statement");
    }
  }
}
