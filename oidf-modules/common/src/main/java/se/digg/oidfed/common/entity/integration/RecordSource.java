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
package se.digg.oidfed.common.entity.integration;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.digg.oidfed.common.entity.integration.registry.ResolverProperties;
import se.digg.oidfed.common.entity.integration.registry.TrustAnchorProperties;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkId;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkIssuerProperties;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkSubjectRecord;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.tree.NodeKey;

import java.util.List;
import java.util.Optional;

public interface RecordSource {
  List<TrustMarkIssuerProperties> getTrustMarkIssuerProperties();

  List<TrustAnchorProperties> getTrustAnchorProperties();

  List<ResolverProperties> getResolverProperties();

  Optional<EntityRecord> getEntity(final NodeKey key);

  List<EntityRecord> getAllEntities();

  List<EntityRecord> findSubordinates(final String issuer);

  //TODO fix trust mark subjects
  List<TrustMarkSubjectRecord> getTrustMarkSubjects(final EntityID issuer, final TrustMarkId id);

  //TODO fix trust mark subjects
  Optional<TrustMarkSubjectRecord> getTrustMarkSubject(final EntityID issuer, final TrustMarkId id, final EntityID subject);

  int priority();
}
