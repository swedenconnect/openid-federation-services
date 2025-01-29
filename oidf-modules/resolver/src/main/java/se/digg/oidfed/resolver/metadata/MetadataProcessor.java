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
package se.digg.oidfed.resolver.metadata;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationCombinationValidator;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationFactory;
import net.minidev.json.JSONObject;

import java.util.List;
import java.util.Objects;

/**
 * Combines metadata policy and calculates what the metadata value should be.
 *
 * @author Felix Hellman
 */
public class MetadataProcessor {

  private final PolicyOperationFactory operationFactory;
  private final PolicyOperationCombinationValidator combinationValidator;

  /**
   * Constructor
   * @param operationFactory for creating operations
   * @param combinationValidator for validating operation combinations
   */
  public MetadataProcessor(final PolicyOperationFactory operationFactory,
      final PolicyOperationCombinationValidator combinationValidator) {
    this.operationFactory = operationFactory;
    this.combinationValidator = combinationValidator;
  }

  /**
   * @param chain to process
   * @return final metadata object
   */
  public JSONObject processMetadata(final List<EntityStatement> chain) {
    try {
      final EntityStatement leafNode = chain.getFirst();

      final List<String> metadataType = leafNode.getClaimsSet()
          .getJSONObjectClaim("metadata")
          .keySet()
          .stream()
          .toList();

      final List<MetadataPolicy> metadataPolicies = chain.stream()
          .flatMap(entity -> {
              return metadataType.stream().map(mdt -> {
                try {
                  return entity.getClaimsSet().getMetadataPolicy(new EntityType(mdt));
                }
                catch (final PolicyViolationException e) {
                  throw new IllegalArgumentException(e);
                }
              });
          })
          .filter(Objects::nonNull)
          .toList();

      final MetadataPolicy combinedMetadataPolicy = MetadataPolicy.combine(metadataPolicies, this.combinationValidator);

      final MetadataPolicy metadataPolicy =
          MetadataPolicy.parse(combinedMetadataPolicy.toJSONObject(), this.operationFactory, this.combinationValidator);

      final JSONObject result = new JSONObject();
      metadataType.forEach(type -> {
        try {
          result.put(type, metadataPolicy.apply(leafNode.getClaimsSet().getMetadata(new EntityType(type))));
        }
        catch (final PolicyViolationException e) {
          throw new RuntimeException(e);
        }
      });
      return result;
    }
    catch (final PolicyViolationException | com.nimbusds.oauth2.sdk.ParseException e) {
      throw new IllegalArgumentException("Failed to validate/parse policy", e);
    }
  }
}
