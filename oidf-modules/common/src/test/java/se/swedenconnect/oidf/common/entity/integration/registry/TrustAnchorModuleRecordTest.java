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

package se.swedenconnect.oidf.common.entity.integration.registry;

import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustAnchorModuleRecord;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testing TrustAnchorModuleRecord
 *
 * @author Per Fredrik Plars
 */
class TrustAnchorModuleRecordTest {

  @Test
  void serde() throws IOException, ClassNotFoundException {

    final TrustAnchorModuleRecord originalRecord = new TrustAnchorModuleRecord();


    byte[] serializedBytes;
    try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
      objectStream.writeObject(originalRecord);
      serializedBytes = byteStream.toByteArray();
    }

    assertNotNull(serializedBytes);
    assertTrue(serializedBytes.length > 0);

    TrustAnchorModuleRecord deserializedRecord;
    try (ByteArrayInputStream byteStream = new ByteArrayInputStream(serializedBytes);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream)) {
      deserializedRecord = (TrustAnchorModuleRecord) objectStream.readObject();
    }
    assertNotNull(deserializedRecord);
  }
}