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

package se.digg.oidfed.trustmarkissuer.dvo;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.digg.oidfed.trustmarkissuer.TestDataSetup.createTrustMarkDelegation;

/**
 * openid-federation-services
 *
 * @author Per Fredrik Plars
 */
class TrustMarkDelegationTest {

  @Test
  void testJavaSerDes() throws IOException, ClassNotFoundException, ParseException, JOSEException {
    final TrustMarkDelegation trId1 = new TrustMarkDelegation(createTrustMarkDelegation().serialize());

    final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOut);
    objectOutputStream.writeObject(trId1);
    objectOutputStream.flush();
    objectOutputStream.close();

    final ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
    final ObjectInputStream objectInputStream = new ObjectInputStream(byteIn);
    final TrustMarkDelegation tm = (TrustMarkDelegation) objectInputStream.readObject();
    objectInputStream.close();

    assertEquals(trId1, tm);

  }

  @Test
  void testParseOK() throws ParseException, JOSEException {
    final SignedJWT signedJWT = createTrustMarkDelegation();
    final TrustMarkDelegation trmd = new TrustMarkDelegation(signedJWT.serialize());
    assertNotNull(trmd.getDelegation());
  }


  @Test
  void testToString() throws ParseException, JOSEException {
    final String jwt = createTrustMarkDelegation().serialize();
    final TrustMarkDelegation trId1 = new TrustMarkDelegation(jwt);
    assertEquals(jwt, trId1.toString());
  }

  @Test
  void testNull() {
    assertThrows(IllegalArgumentException.class,() -> TrustMarkDelegation.create(null));

  }


}