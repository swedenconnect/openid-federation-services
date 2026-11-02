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

import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;

/**
 * Policy operation that is applied to a metadata parameter value of any JSON type.
 *
 * @author Felix Hellman
 */
public interface ExtendedValueOperation extends PolicyOperation {

  /**
   * Applies the operation to a metadata parameter value.
   *
   * @param value the metadata parameter value, null if the parameter is absent
   * @return the resulting value, null if the parameter is to be removed or stays absent
   * @throws PolicyViolationException if the value does not satisfy the operation
   */
  Object apply(Object value) throws PolicyViolationException;
}
