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
package se.swedenconnect.oidf.service.controller;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.util.List;

/**
 * Temp controller.
 *
 * @author Felix Hellman
 */
@RestController
@Slf4j
public class RegistrationController {

  /**
   * Constructor
   * @param registrationSource
   */
  public RegistrationController(final RegistrationSource registrationSource) {
    this.registrationSource = registrationSource;
  }

  private final RegistrationSource registrationSource;

  /**
   * @param request Make the documentation happy
   */
  @PostMapping("/register")
  public void register(@RequestBody final RegistrationRequest request) {
    log.info("Recieved request {}", request);
    this.registrationSource.addEntity(new EntityRecord(
        new EntityID("https://dev.swedenconnect.se/interop/im"),
        request.getSubject(),
        null,
        request.getJwks(),
        null,
        null,
        List.of(),
        List.of()
    ));
  }
}
