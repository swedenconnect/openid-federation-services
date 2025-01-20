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
package se.digg.oidfed.service.trustanchor;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.digg.oidfed.service.IntegrationTestParent;
import se.digg.oidfed.service.testclient.FederationClients;
import se.digg.oidfed.service.testclient.TrustAnchorClient;

import java.text.ParseException;
import java.util.List;

class TrustAnchorIT extends IntegrationTestParent {

  @Test
  void testListSubordinatesAndFetchEntityStatement(final FederationClients clients) throws ParseException {
    final TrustAnchorClient client = clients.authorization().trustAnchor();
    final List<?> body = client.subordinateListing();
    Assertions.assertNotNull(body);
    final SignedJWT signedJWT = client.fetch(new EntityID((String) body.getFirst()));
    Assertions.assertNotNull(signedJWT);
  }
}