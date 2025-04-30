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
package se.swedenconnect.oidf.common.entity.integration;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.RecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.util.List;

class CompositeRecordSourceTest {

  @Test
  void compositeLists() {
    final RecordSource first = Mockito.mock(RecordSource.class);
    Mockito.when(first.findSubordinates(Mockito.anyString()))
        .thenReturn(
            List.of(
                new EntityRecord(new EntityID("http://first"), new EntityID("http://first"), null, null, null, null,
                    null, null, List.of()),
                new EntityRecord(new EntityID("http://second"), new EntityID("http://second"), null, null, null, null
                    , null, null, List.of())
            ));
    final RecordSource second = Mockito.mock(RecordSource.class);
    Mockito.when(second.findSubordinates(Mockito.anyString()))
        .thenReturn(
            List.of(
                new EntityRecord(new EntityID("http://third"), new EntityID("http://third"), null, null, null, null,
                    null, null, List.of()),
                new EntityRecord(new EntityID("http://fourth"), new EntityID("http://fourth"), null, null, null, null
                    , null, null, List.of())
            ));

    final CompositeRecordSource source = new CompositeRecordSource(List.of(first, second));
    final List<EntityRecord> subordinates = source.findSubordinates("any");
    Assertions.assertEquals(4, subordinates.size());
    Assertions.assertTrue(subordinates.stream().anyMatch(e -> e.getIssuer().getValue().equals("http://first")));
    Assertions.assertTrue(subordinates.stream().anyMatch(e -> e.getIssuer().getValue().equals("http://second")));
    Assertions.assertTrue(subordinates.stream().anyMatch(e -> e.getIssuer().getValue().equals("http://third")));
    Assertions.assertTrue(subordinates.stream().anyMatch(e -> e.getIssuer().getValue().equals("http://fourth")));
  }

}