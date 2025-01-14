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
package se.digg.oidfed.service.modules;

import com.nimbusds.jose.JOSEException;
import org.springframework.web.client.RestClient;

import java.text.ParseException;
import java.util.Map;
import java.util.UUID;

/**
 * Integration for loading sub-modules from registry.
 *
 * @author Felix Hellman
 */
public class RestClientSubModuleIntegration {

  private final RestClient client;

  private final SubModuleVerifier verifier;

  /**
   * @param client for communicating with registry.
   * @param verifier to verify payload.
   */
  public RestClientSubModuleIntegration(final RestClient client, final SubModuleVerifier verifier) {
    this.client = client;
    this.verifier = verifier;
  }

  /**
   * @param instanceId of this instance
   * @return response
   * @throws ParseException
   * @throws JOSEException
   */
  public ModuleResponse fetch(final UUID instanceId) throws ParseException, JOSEException {
    final String jwt = this.client.get()
        .uri(builder -> {
          return builder
              .path("/api/v1/federationservice/submodules")
              .query("instance_id={instance_id}")
              .build(Map.of("instance_id", instanceId.toString()));
        }).retrieve()
        .body(String.class);

    return this.verifier.verify(jwt);
  }
}