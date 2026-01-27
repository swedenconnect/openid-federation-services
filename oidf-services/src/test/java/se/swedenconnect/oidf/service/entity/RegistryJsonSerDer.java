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
package se.swedenconnect.oidf.service.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkOwner;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ConstraintRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.NamingConstraints;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSourceProperty;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;

import java.util.List;
import java.util.Map;

public class RegistryJsonSerDer {
  @Test
  void moduleRecord() throws JsonProcessingException {
    final ModuleRecord build = ModuleRecord.builder()
        .resolvers(List.of(ResolverProperties.builder()
            .trustAnchor("http://ta.test")
            .entityIdentifier("http://resolver.test")
            .build())
        )
        .trustMarkIssuers(List.of(TrustMarkIssuerProperties.builder().entityIdentifier(new EntityID("http://tmi.test")).trustMarks(
            List.of(
                TrustMarkProperties.builder().trustMarkType(new TrustMarkType("http://tmi.test/tm"))
                    .trustMarkSubjects(List.of(TrustMarkSubjectProperty.builder().sub("http://first.test").build())).build())).build())
        )
        .trustAnchors(List.of(TrustAnchorProperties.builder()
            .entityIdentifier(new EntityID("http://ta.test"))
            .trustMarkOwners(List.of(TrustMarkOwner.builder().build()))
            .trustMarkIssuers(Map.of(new EntityID("http://tmi.test/tm"), List.of(new EntityID("http://tmi.test"))))
            .subordinates(
                List.of(
                    TrustAnchorProperties.SubordinateListingProperty.fromEntityId(
                        new EntityID("http://first.test")
                    ),
                    TrustAnchorProperties.SubordinateListingProperty.fromEntityId(
                        new EntityID("http://second.test")
                    ),
                    TrustAnchorProperties.SubordinateListingProperty.builder()
                        .entityIdentifier(new EntityID("http://third.test"))
                        .constraints(ConstraintRecord.builder().allowedEntityTypes(List.of("entity_type")).maxPathLength(5L)
                            .naming(NamingConstraints.builder().permitted(List.of(
                                "permitted")).excluded(List.of("exlucded")).build()).build())
                        .build())
            ).build())
        )
        .build();

    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    final String s = objectMapper.writeValueAsString(build);
    System.out.println(s);
  }

  @Test
  void entityRecord() throws JsonProcessingException {
    final EntityRecord entityRecord = EntityRecord.builder()
        .entityIdentifier(new EntityID("http://first.test"))
        .crit(List.of("crit"))
        .trustMarkSource(List.of(new TrustMarkSourceProperty(new EntityID("http://tmi.test"), "http://tmi.test/tm")))
        .authorityHints(List.of("http://ta.test"))
        .metadata(Map.of())
        .ecLocation("http://otherlocation.test")
        .build();

    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    final String s = objectMapper.writeValueAsString(entityRecord);
    System.out.println(s);
  }
}
