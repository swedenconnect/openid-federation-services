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
package se.swedenconnect.oidf.common.entity.entity.integration.trustmark;

import com.nimbusds.jwt.SignedJWT;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link TrustMarkStatusStore}.
 *
 * @author Felix Hellman
 */
public class InMemoryTrustMarkStatusStore implements TrustMarkStatusStore {

  private final ConcurrentHashMap<String, TrustMarkStatusResponse> store = new ConcurrentHashMap<>();

  private String key(final String subject, final String trustMarkType) {
    return subject + ":" + trustMarkType;
  }

  @Override
  public void setTrustMarkStatus(final String subject, final String trustMarkType,
                                 final TrustMarkStatusResponse trustMarkStatus) {
    this.store.put(this.key(subject, trustMarkType), trustMarkStatus);
  }

  @Override
  public Optional<TrustMarkStatusResponse> getTrustMarkStatus(final String subject, final String trustMarkType) {
    return Optional.ofNullable(this.store.get(this.key(subject, trustMarkType)));
  }

  @Override
  public void clear() {
    this.store.clear();
  }
}
