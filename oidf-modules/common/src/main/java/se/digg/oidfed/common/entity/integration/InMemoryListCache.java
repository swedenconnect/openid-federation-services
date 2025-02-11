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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory implementation of {@link ListCache}
 * @param <K> class for key
 * @param <V> class for value
 *
 * @author Felix Hellman
 */
public class InMemoryListCache<K, V> implements ListCache<K, V> {
  private final Map<K, List<V>> cache = new ConcurrentHashMap<>();

  @Override
  public void append(final K key, final V value) {
    Optional.ofNullable(this.cache.get(key))
        .ifPresentOrElse(list -> {
              list.add(value);
              this.cache.put(key, list);
            },
            () -> {
              final ArrayList<V> list = new ArrayList<>();
              list.add(value);
              this.cache.put(key, list);
            });
  }

  @Override
  public List<V> getAll(final K key) {
    return Optional.ofNullable(this.cache.get(key))
        .orElse(List.of());
  }
}
