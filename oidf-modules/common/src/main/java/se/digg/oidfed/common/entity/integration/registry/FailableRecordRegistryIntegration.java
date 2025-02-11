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

import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;
import se.digg.oidfed.common.entity.integration.Expirable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements a record registry integration that can retry errors.
 *
 * @author Felix Hellman
 */
public class FailableRecordRegistryIntegration {
  private final RecordRegistryIntegration integration;

  /**
   * @param integration for doing actual requests
   */
  public FailableRecordRegistryIntegration(final RecordRegistryIntegration integration) {
    this.integration = integration;
  }

  /**
   * Fetch {@link PolicyRecord}
   * @param id of policy
   * @return policy
   */
  public Failable<Expirable<PolicyRecord>> getPolicy(final String id) {
    try {
      return new Failable<>(this.integration.getPolicy(id), null);
    } catch (final RegistryResponseException e) {
      return new Failable<>(null, e);
    }
  }

  /**
   * @param issuer for the entity records
   * @return list of entity records
   */
  public Failable<Expirable<List<EntityRecord>>> getEntityRecords(final String issuer) {
    try {
      return new Failable<>(this.integration.getEntityRecords(issuer), null);
    } catch (final RegistryResponseException e) {
      return new Failable<>(null, e);
    }
  }

  /**
   * @param instanceId of this instance
   * @return modules for this instance
   */
  public Failable<Expirable<ModuleResponse>> getModules(final UUID instanceId) {
    try {
      return new Failable<>(this.integration.getModules(instanceId), null);
    } catch (final RegistryResponseException e) {
      return new Failable<>(null, e);
    }
  }

  /**
   * @param issuer for trust mark
   * @param trustMarkId for trust mark
   * @return list of trust mark subjects
   */
  public Failable<Expirable<List<TrustMarkSubject>>> getTrustMarkSubject(final String issuer,
                                                                         final String trustMarkId) {
    try {
      return new Failable<>(this.integration.getTrustMarkSubject(issuer, trustMarkId), null);
    } catch (final RegistryResponseException e) {
      return new Failable<>(null, e);
    }
  }

  /**
   * Wrapper for doing lambda style error handling.
   * @param <V>
   *
   * @author Felix Hellman
   */
  public static final class Failable<V> {
    private final V value;
    private final Exception e;

    /**
     * @param value for response
     * @param e for response
     */
    public Failable(final V value, final Exception e) {
      this.value = value;
      this.e = e;
    }

    /**
     * @param runnable to run on failure
     * @return value if exception is null
     */
    public Optional<V> onFailure(final Runnable runnable) {
      if (Objects.isNull(this.e)) {
        return Optional.of(this.value);
      }
      runnable.run();
      return Optional.empty();
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      final Failable that = (Failable) obj;
      return Objects.equals(this.value, that.value) &&
          Objects.equals(this.e, that.e);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.value, this.e);
    }

    @Override
    public String toString() {
      return "Failable[" +
          "value=" + this.value + ", " +
          "e=" + this.e + ']';
    }

  }
}
