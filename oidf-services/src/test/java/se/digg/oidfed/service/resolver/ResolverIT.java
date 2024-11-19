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
package se.digg.oidfed.service.resolver;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.digg.oidfed.resolver.ResolverRequest;
import se.digg.oidfed.service.IntegrationTestParent;

import java.text.ParseException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class ResolverIT extends IntegrationTestParent {


  @Test
  void resolveFederation() throws ParseException {

    final ResolverClient resolverClient = new ResolverClient(serverPort);
    final String resolved = resolverClient.resolve(new ResolverRequest(
        "http://localhost:9090/intermediate/relyingparty",
        "http://localhost:9090/trustanchor", "")
    );
    SignedJWT.parse(resolved);
  }
}