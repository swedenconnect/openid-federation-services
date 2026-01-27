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
package se.swedenconnect.oidf.service.time;

import lombok.AllArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

@AllArgsConstructor
public class TestClock extends Clock {

  private final Clock clock;
  private Instant stoppedTime = null;

  @Override
  public ZoneId getZone() {
    return this.clock.getZone();
  }

  @Override
  public Clock withZone(final ZoneId zone) {
    return this.clock.withZone(zone);
  }

  @Override
  public Instant instant() {
    return Optional.ofNullable(this.stoppedTime)
        .orElse(this.clock.instant());
  }

  public void stopTime(final Instant stoppedTime) {
    this.stoppedTime = stoppedTime;
  }

  public void resumseTime() {
    this.stoppedTime = null;
  }
}
