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
package se.digg.oidfed.service.testclient;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.web.client.RestClient;

public class EntityClient {
  private final RestClient client;

  public EntityClient(final RestClient client) {
    this.client = client;
  }

  public EntityStatement getEntityConfiguration(final EntityID entity) {
    try {
    final String body = client.get().uri(entity.getValue() + "/.well-known/openid-federation")
        .retrieve()
        .body(String.class);
    return EntityStatement.parse(body);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to fetch entity configiration for entity %s".formatted(entity.getValue()));
    }
  }
}
