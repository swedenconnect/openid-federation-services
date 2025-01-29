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
package se.digg.oidfed.service.modules;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkDelegation;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TrustMarkIssuer module from registry.
 *
 * @author Felix Hellman
 */
@Getter
public class TrustMarkIssuerModuleResponse {
  private Duration trustMarkValidityDuration;
  private EntityID entityIdentifier;
  private JWK jwk;
  private List<TrustMarkResponse> trustMarks;
  private String alias;
  private Boolean active;

  /**
   * Converts json object {@link java.util.HashMap} to new instance
   * @param json to read
   * @return new instance
   */
  public static TrustMarkIssuerModuleResponse fromJson(final Map<String, Object> json) {
    return new TrustMarkIssuerModuleResponse();
  }

  /**
   * @param trustMarkId
   * @param logoUri
   * @param refUri
   * @param delegation
   */
  public record TrustMarkResponse(TrustMarkId trustMarkId, Optional<String> logoUri, Optional<String> refUri,
                                  Optional<TrustMarkDelegation> delegation) {}
}
