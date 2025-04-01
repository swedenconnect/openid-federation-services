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
import java.util.Optional;
import java.util.Set;

/**
 * Validates crit and metadata_policy_crit claims of the chain.
 *
 * @author Felix Hellman
 */
public class CriticalClaimsValidationStep implements ChainValidationStep {

  /**
   * This implementation supports the subject_entity_configuration_location claim.
   */
  public static final Set<String> SUPPORTED_CRITICAL_CLAIMS =
      Set.of("subject_entity_configuration_location");

  /**
   * This implementation supports the additional metadata operators regexp and instersects.
   */
  public static final Set<String> SUPPORTED_METADATA_CLAIMS = Set.of(
      "regexp", "intersects"
  );

  @Override
  public void validate(final List<EntityStatement> chain) {
    chain
        .forEach(es -> {
          Optional.ofNullable(es.getClaimsSet().getCriticalExtensionClaims())
              .ifPresent(crit -> {
                if (!new HashSet<>(crit).containsAll(SUPPORTED_CRITICAL_CLAIMS)) {
                  throw new IllegalArgumentException("Unsupported critical claims declaration in Entity Statement");
                }
              });
          Optional.ofNullable(es.getClaimsSet().getStringListClaim("metadata_policy_crit"))
              .ifPresent(critMetadata -> {
                if (!new HashSet<>(critMetadata).containsAll(SUPPORTED_METADATA_CLAIMS)) {
                  throw new IllegalArgumentException("Unsupported critical claims declaration in Entity Statement");
                }
              });
        });
  }
}
