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
package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.common.exception.NotFoundException;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory implementaiton of {@link TrustMarkSubjectRepository}
 *
 * @author Felix Hellman
 */
@Slf4j
public class InMemoryTrustMarkSubjectRepository implements TrustMarkSubjectRepository {

  private final Map<String, List<TrustMarkSubject>> trustMarkIdToSubjectList = new ConcurrentHashMap<>();
  private final Clock clock;

  /**
   * Constructor.
   * @param clock
   */
  public InMemoryTrustMarkSubjectRepository(final Clock clock) {
    this.clock = clock;
  }

  @Override
  public void register(final TrustMarkId trustMarkId, final TrustMarkSubject subject) {
    if (!this.trustMarkIdToSubjectList.containsKey(trustMarkId.getTrustMarkId())) {
      this.trustMarkIdToSubjectList.put(trustMarkId.getTrustMarkId(), new ArrayList<>());
    }
    final List<TrustMarkSubject> trustMarkSubjects = this.trustMarkIdToSubjectList.get(trustMarkId.getTrustMarkId());
    trustMarkSubjects.add(subject);
    log.debug("Registered subject {}", subject);
    this.trustMarkIdToSubjectList.put(trustMarkId.getTrustMarkId(), trustMarkSubjects);
  }

  @Override
  public List<TrustMarkSubject> getAll(final TrustMarkId trustMarkId) {
    return Optional.ofNullable(this.trustMarkIdToSubjectList.get(trustMarkId.getTrustMarkId()))
        .map(subjectList -> subjectList.stream().filter(this::isSubjectValid).toList())
        .orElseGet(List::of);
  }

  @Override
  public Optional<TrustMarkSubject> getSubject(final TrustMarkId trustMarkId, final EntityID subject) {
    final List<TrustMarkSubject> trustMarkSubjects = this.trustMarkIdToSubjectList.get(trustMarkId.getTrustMarkId());
    if (trustMarkSubjects != null) {
      return trustMarkSubjects.stream()
          .filter(s -> s.sub().equals(subject.getValue()))
          .filter(this::isSubjectValid)
          .findFirst();
    }
    return Optional.empty();
  }

  /**
   * Testing that this trustmark is valid. That it has not expired or that it is revoked
   *
   * @param trustMarkSubjectRecord Trustmark record
   * @return True if valid false otherwise
   */
  private boolean isSubjectValid(final TrustMarkSubject trustMarkSubjectRecord) {
    if (trustMarkSubjectRecord.revoked()) {
      log.debug("Trust Mark is revoked. sub:'{}'", trustMarkSubjectRecord.sub());
      return false;
    }

    if (Optional.ofNullable(trustMarkSubjectRecord.expires())
        .filter(expires -> Instant.now(this.clock).isAfter(expires))
        .isPresent()) {
      log.debug("Trust Mark for sub:'{}' has expired", trustMarkSubjectRecord.sub());
      return false;
    }

    if (Optional.ofNullable(trustMarkSubjectRecord.granted())
        .filter(granted -> Instant.now(this.clock).isBefore(granted))
        .isPresent()) {
      log.debug("Trust Mark for sub:'{}' is not yet granted", trustMarkSubjectRecord.sub());
      return false;
    }

    log.debug("Trust Mark for sub:'{}' is valid", trustMarkSubjectRecord.sub());
    return true;
  }
}
