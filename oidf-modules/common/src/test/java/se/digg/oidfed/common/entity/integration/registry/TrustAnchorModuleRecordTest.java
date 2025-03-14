package se.digg.oidfed.common.entity.integration.registry;

import org.junit.jupiter.api.Test;

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