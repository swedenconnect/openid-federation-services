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

import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * @param <V> type
 * @author Felix Hellman
 */
@Getter
public class Expirable<V> implements Serializable {

  private final Instant expiration;
  private final V value;

  /**
   * Constructor.
   *
   * @param expiration when this value is no longer value
   * @param value      contained in the container
   */
  public Expirable(final Instant expiration, final V value) {
    this.expiration = expiration;
    this.value = value;
  }

  /**
   * "non expiring" value with a lifetime of 10 years.
   *
   * @param v   value
   * @param <V> type
   * @return expirable that expires after ten years.
   */
  public static <V> Expirable<V> nonExpiring(final V v) {
    return new Expirable<>(Instant.now().plus(365 * 10, ChronoUnit.DAYS), v);
  }
}
