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
package se.swedenconnect.oidf.trustanchor;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.exception.InvalidIssuerException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;

import java.util.List;

/**
 * Interface for trust anchor.
 *
 * @author Felix Hellman
 */
public interface TrustAnchor {

  /**
   * @param request to fetch entity statement for
   * @return entity statement
   * @throws InvalidIssuerException
   * @throws NotFoundException
   */
  String fetchEntityStatement(FetchRequest request) throws InvalidIssuerException, NotFoundException;

  /**
   * @param request to get subordinate listing for current module
   * @return listing of subordinates
   * @throws FederationException when loading entity configurations fails
   */
  List<String> subordinateListing(SubordinateListingRequest request) throws FederationException;

  /**
   * @return entity id of this trust anchor
   */
  EntityID getEntityId();
}
