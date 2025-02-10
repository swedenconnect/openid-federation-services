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

import com.nimbusds.oauth2.sdk.ParseException;
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
  private Duration trustMarkTokenValidityDuration;
  private EntityID entityIdentifier;

  private String alias;
  private Boolean active;
  private List<TrustMarkResponse> trustMarks;

  /**
   * Converts json object {@link java.util.HashMap} to new instance
   * @param json to read
   * @return new instance
   */
  public static TrustMarkIssuerModuleResponse fromJson(final Map<String, Object> json) {
    final TrustMarkIssuerModuleResponse response = new TrustMarkIssuerModuleResponse();

    try {
      response.entityIdentifier = EntityID.parse((String) json.get("entity-identifier"));

      response.alias = (String) json.get("alias");
      response.active = (Boolean) json.get("active");
      response.trustMarkTokenValidityDuration =
          Duration.parse((String) json.get("trust-mark-token-validity-duration"));
      //TODO: Implement Trustmark

    }
    catch (ParseException e) {
      throw new IllegalArgumentException("Unable to parse data from registry",e);
    }
    return response;
  }

  /**
   * Represents a response containing information about a Trust Mark issued
   * by an entity. This class serves as a data structure to encapsulate
   * the details of a Trust Mark, including its identifier, optional logo URI,
   * optional reference URI, and optional delegation details.
   * This record is immutable and holds the following components:
   * - {@code TrustMarkId trustMarkId}: A unique identifier for the Trust Mark.
   * - {@code Optional<String> logoUri}: An optional URI for the logo associated with the Trust Mark.
   * - {@code Optional<String> refUri}: An optional reference URI for more details about the Trust Mark.
   * - {@code Optional<TrustMarkDelegation> delegation}: An optional delegation structure providing details
   *   about how the trust is delegated.
   */
  public record TrustMarkResponse(TrustMarkId trustMarkId, Optional<String> logoUri, Optional<String> refUri,
                                  Optional<TrustMarkDelegation> delegation) {}
}
