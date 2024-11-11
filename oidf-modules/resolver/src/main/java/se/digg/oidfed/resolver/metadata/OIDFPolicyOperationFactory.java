/*
 * Copyright 2024 Sweden Connect
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

import com.nimbusds.openid.connect.sdk.federation.policy.language.OperationName;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationFactory;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationFactory;
import se.digg.oidfed.resolver.metadata.operations.IntersectsOperation;
import se.digg.oidfed.resolver.metadata.operations.RegexpOperation;

/**
 * Implementation of {@link PolicyOperationFactory} that extends the {@link DefaultPolicyOperationFactory} with
 * operations "regexp" and "intersects"
 *
 * @author Felix Hellman
 */
public class OIDFPolicyOperationFactory implements PolicyOperationFactory {

  private final DefaultPolicyOperationFactory operationFactory = new DefaultPolicyOperationFactory();

  @Override
  public PolicyOperation createForName(final OperationName operationName) {
    return switch (operationName.getValue()) {
      case "regexp" -> new RegexpOperation();
      case "intersects" -> new IntersectsOperation();
      default -> operationFactory.createForName(operationName);
    };
  }
}
