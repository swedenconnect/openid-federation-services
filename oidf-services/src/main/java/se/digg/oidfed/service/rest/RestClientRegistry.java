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
package se.digg.oidfed.service.rest;

import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry class for rest clients.
 *
 * @author Felix Hellman
 */
public class RestClientRegistry {
  private final Map<String, RestClient> restClients = new HashMap<>();

  /**
   * @param name of the client
   * @param client the client to register
   */
  public void register(final String name, final RestClient client) {
    restClients.put(name, client);
  }

  /**
   * @param name of the client
   * @return instance of client if it exists
   */
  public Optional<RestClient> getClient(final String name) {
    return Optional.ofNullable(restClients.get(name));
  }
}
