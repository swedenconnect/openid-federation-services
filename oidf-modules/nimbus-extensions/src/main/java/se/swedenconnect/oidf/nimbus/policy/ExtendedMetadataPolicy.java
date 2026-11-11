/*
 * Copyright 2026 Sweden Connect
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
package se.swedenconnect.oidf.nimbus.policy;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import net.minidev.json.JSONObject;

/**
 * Parses metadata policies with the extended {@code value} and {@code one_of} operators.
 *
 * @author Felix Hellman
 */
public final class ExtendedMetadataPolicy {

  private ExtendedMetadataPolicy() {
  }

  /**
   * Parses a metadata policy for one entity type.
   *
   * @param policySpec the metadata policy JSON object
   * @return the metadata policy
   * @throws ParseException if the policy cannot be parsed
   * @throws PolicyViolationException if an operator is not supported or the operators cannot be combined
   */
  public static MetadataPolicy parse(final JSONObject policySpec) throws ParseException, PolicyViolationException {
    return MetadataPolicy.parse(policySpec, new ExtendedPolicyOperationFactory(),
        new ExtendedPolicyOperationCombinationValidator());
  }
}
