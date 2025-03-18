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

package se.swedenconnect.oidf.trustmarkissuer.dvo;

import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Federation Domain Value Object TrustMarkId tests
 *
 * @author Per Fredrik Plars
 */
class TrustMarkIdTest {

  @Test
  void testJavaSerDes() throws IOException, ClassNotFoundException {
    final TrustMarkId trId1 = new TrustMarkId("http://www.tm.se/1");

    final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOut);
    objectOutputStream.writeObject(trId1);
    objectOutputStream.flush();
    objectOutputStream.close();

    final ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
    final ObjectInputStream objectInputStream = new ObjectInputStream(byteIn);
    final TrustMarkId tm = (TrustMarkId) objectInputStream.readObject();
    objectInputStream.close();

    assertEquals(trId1, tm);

  }

  @Test
  void testToString() {
    final TrustMarkId trId1 = new TrustMarkId("http://www.tm.se/1");
    assertEquals("http://www.tm.se/1", trId1.toString());
  }

  @Test
  void testNull() {
    assertThrows(IllegalArgumentException.class,() -> TrustMarkId.create(null));

  }

  @Test
  void testEquals() {
    final TrustMarkId trId1 = new TrustMarkId("http://www.tm.se/1");
    final TrustMarkId trId2 = new TrustMarkId("http://www.tm.se/2");
    assertNotEquals(trId1, trId2);
    assertEquals(trId1, trId1);
  }

  @Test
  void testHashCode() {
    final TrustMarkId trId1 = new TrustMarkId("http://www.tm.se/1");
    final TrustMarkId trId2 = new TrustMarkId("http://www.tm.se/2");
    assertNotEquals(trId1.hashCode(), trId2.hashCode());
    assertEquals(trId1.hashCode(), trId1.hashCode());
  }

  @Test
  void getId() {
    final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TrustMarkId("Gurka"));
    assertEquals("Unable to create TrustMarkId. For input: 'Gurka'", ex.getMessage());
    new TrustMarkId("http://www.gurka.se");
  }
}