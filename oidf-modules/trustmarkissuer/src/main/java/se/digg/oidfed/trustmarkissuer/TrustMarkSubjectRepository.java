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
package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.util.List;
import java.util.Optional;

/**
 * Subject repository for trust marks.
 *
 * @author Felix Hellman
 */
public interface TrustMarkSubjectRepository {
  /**
   * @param trustMarkId to search
   * @return all subjects for this id
   */
  List<TrustMarkSubject> getAll(final TrustMarkId trustMarkId);

  /**
   * @param trustMarkId to search
   * @param subject to find
   * @return optional of subject if it exists
   */
  Optional<TrustMarkSubject> getSubject(final TrustMarkId trustMarkId, final EntityID subject);

  /**
   * Registers a subject to a trust mark
   * @param trustMarkId to register for
   * @param subject to register
   */
  void register(final TrustMarkId trustMarkId, final TrustMarkSubject subject);
}
