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
package se.swedenconnect.oidf.service.trustanchor;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkOwner;

import java.text.ParseException;

/**
 * Trust Mark Owner Properties class.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class TrustMarkOwnerProperties {
  private String trustMarkId;
  private TrustMarkOwnerProperty trustMarkOwner;

  /**
   * @return new instance
   * @throws ParseException
   */
  public TrustMarkOwner toTrustMarkOwner() throws ParseException {
    return new TrustMarkOwner(
        new EntityID(this.trustMarkOwner.getSub()),
        JWKSet.parse(this.trustMarkOwner.getJwks().getJson()).toPublicJWKSet()
    );
  }
}
