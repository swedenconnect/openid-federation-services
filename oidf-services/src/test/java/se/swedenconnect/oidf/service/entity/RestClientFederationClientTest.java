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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;

import java.util.Map;

public class RestClientFederationClientTest {

  @Test
  void testEmbedded() {
    final FederationClient federationClient = new se.swedenconnect.oidf.service.entity.RestClientFederationClient(RestClient.builder().build());
    final String embedded = """
        data:application/entity-statement+jwt,eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdXRob3JpdHlfaGludHMiOlsiaHR0cHM6Ly91bXUuc2UiXSwiZXhwIjoxNTY4Mzk3MjQ3LCJpYXQiOjE1NjgzMTA4NDcsImlzcyI6Imh0dHBzOi8vb3AudW11LnNlIiwic3ViIjoiaHR0cHM6Ly9vcC51bXUuc2UiLCJqd2tzIjp7ImtleXMiOlt7ImUiOiJBUUFCIiwia2lkIjoiZEVFdFJqbHpZM2RqY0VOdVQwMXdPR3hyWmxreGIzUklRVkpsTVRZMC4uLiIsImt0eSI6IlJTQSIsIm4iOiJ4OTdZS3FjOUNzLUROdEZyUTdfdmhYb0g5YndrRFdXNkVuMmpKMDQ0eUguLi4ifV19LCJtZXRhZGF0YSI6eyJvcGVuaWRfcHJvdmlkZXIiOnsiaXNzdWVyIjoiaHR0cHM6Ly9vcC51bXUuc2Uvb3BlbmlkIiwic2lnbmVkX2p3a3NfdXJpIjoiaHR0cHM6Ly9vcC51bXUuc2Uvb3BlbmlkL2p3a3Muam9zZSIsImF1dGhvcml6YXRpb25fZW5kcG9pbnQiOiJodHRwczovL29wLnVtdS5zZS9vcGVuaWQvYXV0aG9yaXphdGlvbiIsImNsaWVudF9yZWdpc3RyYXRpb25fdHlwZXNfc3VwcG9ydGVkIjpbImF1dG9tYXRpYyIsImV4cGxpY2l0Il0sInJlcXVlc3RfcGFyYW1ldGVyX3N1cHBvcnRlZCI6dHJ1ZSwiZ3JhbnRfdHlwZXNfc3VwcG9ydGVkIjpbImF1dGhvcml6YXRpb25fY29kZSIsImltcGxpY2l0IiwidXJuOmlldGY6cGFyYW1zOm9hdXRoOmdyYW50LXR5cGU6and0LWJlYXJlciJdLCJpZF90b2tlbl9zaWduaW5nX2FsZ192YWx1ZXNfc3VwcG9ydGVkIjpbIkVTMjU2IiwiUlMyNTYiXSwibG9nb191cmkiOiJodHRwczovL3d3dy51bXUuc2UvaW1nL3VtdS1sb2dvLWxlZnQtbmVnLVNFLnN2ZyIsIm9wX3BvbGljeV91cmkiOiJodHRwczovL3d3dy51bXUuc2UvZW4vd2Vic2l0ZS9sZWdhbC1pbmZvcm1hdGlvbi8iLCJyZXNwb25zZV90eXBlc19zdXBwb3J0ZWQiOlsiY29kZSIsImNvZGUgaWRfdG9rZW4iLCJ0b2tlbiJdLCJzdWJqZWN0X3R5cGVzX3N1cHBvcnRlZCI6WyJwYWlyd2lzZSIsInB1YmxpYyJdLCJ0b2tlbl9lbmRwb2ludCI6Imh0dHBzOi8vb3AudW11LnNlL29wZW5pZC90b2tlbiIsImZlZGVyYXRpb25fcmVnaXN0cmF0aW9uX2VuZHBvaW50IjoiaHR0cHM6Ly9vcC51bXUuc2Uvb3BlbmlkL2ZlZHJlZyIsInRva2VuX2VuZHBvaW50X2F1dGhfbWV0aG9kc19zdXBwb3J0ZWQiOlsiY2xpZW50X3NlY3JldF9wb3N0IiwiY2xpZW50X3NlY3JldF9iYXNpYyIsImNsaWVudF9zZWNyZXRfand0IiwicHJpdmF0ZV9rZXlfand0Il19fX0.c7khQIUivEJ9o6LuFOML5yhiYIDotojwwd5yr3GwGYU
        """;
    final EntityStatement entityStatement = federationClient.entityConfiguration(new FederationRequest<>(new EntityConfigurationRequest(new EntityID(
        "https://unsused"), embedded),
        Map.of(), true));

    Assertions.assertEquals("https://op.umu.se", entityStatement.getEntityID().getValue());
  }

}