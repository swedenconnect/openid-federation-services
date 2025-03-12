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
package se.digg.oidfed.service.state;

/**
 * Distributed locking to sync jobs between multiple nodes in the same instance.
 *
 * @author Felix Hellman
 */
public interface ServiceLock {
  /**
   * @param name of the lock
   * @return true if the lock was aquired.
   */
  boolean acquireLock(final String name);

  /**
   * @param name of the lock to close
   */
  void close(final String name);
}
