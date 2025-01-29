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
package se.digg.oidfed.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reports active modules at startup.
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
public class ModuleReporter {

  private final List<ApplicationModule> modules;

  /**
   * Constructor.
   *
   * @param modules
   */
  public ModuleReporter(final List<ApplicationModule> modules) {
    this.modules = modules;
  }

  /**
   * Reports information about modules after startup.
   */
  @PostConstruct
  public void reportModules() {
    this.modules.forEach(module -> log.info("Registered module {} at startup", module.getModuleName()));
  }
}
