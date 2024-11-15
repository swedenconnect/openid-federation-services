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
 *  limitations under the License.
 */

package se.digg.oidfed.trustmarkissuer;

import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.util.List;
import java.util.Optional;

/**
 * Interface that is called when new subject needs to load.
 *
 * @author Per Fredrik Plars
 */
public interface TrustMarkIssuerSubjectLoader {
  /**
   * Expected to load TrustMarkIssuerSubjectProperties
   * @param issuerEntityId IssuerEntityId
   * @param trustMarkId TrustMarkId
   * @param subject Subject
   * @return Empty list if there is no data.
   */
  List<TrustMarkIssuerSubject> loadSubject(String issuerEntityId,
      TrustMarkId trustMarkId, Optional<String> subject);
}
