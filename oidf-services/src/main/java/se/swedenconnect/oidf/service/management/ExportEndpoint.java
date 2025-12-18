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
package se.swedenconnect.oidf.service.management;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import se.swedenconnect.oidf.service.resolver.cache.ResolverCacheRegistry;
import se.swedenconnect.oidf.service.state.StateHashFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Export endpoint for oidf service.
 *
 * @author Felix Hellman
 */
@Endpoint(id = "export")
@Component
@AllArgsConstructor
public class ExportEndpoint {
  private final ResolverCacheRegistry registry;
  private final CompositeRecordSource source;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    MAPPER.registerModule(new JavaTimeModule());

    final SimpleModule module = new SimpleModule();
    module.addSerializer(JWKSet.class, new StateHashFactory.JWKSetSerializer());
    module.addDeserializer(JWKSet.class, new StateHashFactory.JWKSetDeserializer());
    MAPPER.registerModule(module);
  }

  /**
   * Exports federation as json
   * @return federation as json
   * @throws JsonProcessingException
   */
  @ReadOperation
  public String exportFederation() throws JsonProcessingException {
    final ResolverProperties properties = this.source.getResolverProperties().getFirst();

    final EntityStatementTree tree = this.registry.getRegistration(properties.entityIdentifier()).get().tree();
    final Set<EntityStatement> result = tree.getAll()
        .stream()
        .map(Tree.SearchResult::getData)
        .collect(Collectors.toSet());
    return this.MAPPER.writeValueAsString(result);
  }
}
