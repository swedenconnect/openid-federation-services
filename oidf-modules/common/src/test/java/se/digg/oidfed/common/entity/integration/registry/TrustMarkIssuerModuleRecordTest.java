package se.digg.oidfed.common.entity.integration.registry;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testing TrustMarkIssuerModuleRecord
 *
 * @author Per Fredrik Plars
 */
class TrustMarkIssuerModuleRecordTest {

  @Test
  void serde() throws IOException, ClassNotFoundException {

    final TrustMarkIssuerModuleRecord originalRecord = new TrustMarkIssuerModuleRecord(Duration.ofDays(1)
    ,"",
        Collections.emptyList(),
        true);


    byte[] serializedBytes;
    try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
      objectStream.writeObject(originalRecord);
      serializedBytes = byteStream.toByteArray();
    }

    assertNotNull(serializedBytes);
    assertTrue(serializedBytes.length > 0);

    TrustMarkIssuerModuleRecord deserializedRecord;
    try (ByteArrayInputStream byteStream = new ByteArrayInputStream(serializedBytes);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream)) {
      deserializedRecord = (TrustMarkIssuerModuleRecord) objectStream.readObject();
    }
    assertNotNull(deserializedRecord);
  }
}