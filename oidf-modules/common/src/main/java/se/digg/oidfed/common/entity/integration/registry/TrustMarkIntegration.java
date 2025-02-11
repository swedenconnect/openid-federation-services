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
package se.digg.oidfed.common.entity.integration.registry;

import com.nimbusds.jwt.SignedJWT;
import se.digg.oidfed.common.entity.integration.federation.TrustMarkRequest;

/**
 * Integration for fetching trust marks from Trust Mark Issuer.
 *
 * @author Felix Hellman
 */
public interface TrustMarkIntegration {
  /**
   * @param request for a trust mark
   * @return trust mark
   */
  SignedJWT getTrustMark(final TrustMarkRequest request);
}
