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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.ParseException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.digg.oidfed.resolver.Discovery;
import se.digg.oidfed.resolver.DiscoveryRequest;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.resolver.ResolverRequest;
import se.digg.oidfed.service.ApplicationModule;
import se.digg.oidfed.service.submodule.ResolverModuleRepository;

import java.util.List;

/**
 * Controller for Resolver.
 *
 * @author Felix Hellman
 */
@RestController
@ConditionalOnProperty(value = ResolverConfigurationProperties.PROPERTY_PATH + ".active", havingValue = "true")
public class ResolverController implements ApplicationModule {

  private final ResolverModuleRepository repository;

  /**
   * Constructor.
   *
   * @param repository for resolver modules
   */
  public ResolverController(final ResolverModuleRepository repository) {
    this.repository = repository;
  }

  /**
   * Resolves the entity based on the given parameters.
   *
   * @param alias the alias of the resolver providing the response
   * @param subject the resolved subject entity
   * @param trustAnchor the trust anchor to resolve to
   * @param type the type of the entity (optional) for filtering metadata
   * @return the resolved entity as a JSON response or an error response
   */
  @GetMapping(value = "/{alias}/resolve", produces = "application/resolve-response+jwt")
  public String resolveEntity(
      @PathVariable(name = "alias") String alias,
      @RequestParam(name = "sub", required = false) String subject,
      @RequestParam(name = "anchor", required = false) String trustAnchor,
      @RequestParam(name = "type", required = false) String type) {
    final ResolverRequest resolverRequest = new ResolverRequest(subject, trustAnchor, type);
    final Resolver resolver = this.repository.getResolver(alias)
        .orElseThrow(() -> new IllegalArgumentException("Could not find resolver with alias %s".formatted(alias)));
    return resolver.resolve(resolverRequest);
  }

  /**
   * Retrieves the discovery information about resolvable entities.
   *
   * @param alias the alias of the resolver providing the response
   * @param trustAnchor the trust anchor to use for resolving entities
   * @param types a list of types to filter the discovery information (optional)
   * @param trustMarkIds a list of trust mark IDs to filter the discovery information (optional)
   * @return a ResponseEntity containing a list of resolvable entities as a JSON array
   */
  @GetMapping(value = "/{alias}/discovery", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> discovery(
      @PathVariable(name = "alias") String alias,
      @RequestParam(name = "anchor", required = false) String trustAnchor,
      @RequestParam(name = "type", required = false) List<String> types,
      @RequestParam(name = "trust_mark_id", required = false) List<String> trustMarkIds) {
    final Resolver resolver = repository.getResolver(alias)
        .orElseThrow(() -> new IllegalArgumentException("Could not find resolver with alias %s".formatted(alias)));
    return resolver.discovery(new DiscoveryRequest(trustAnchor, types, trustMarkIds)).supportedEntities();
  }

  @Override
  public String getModuleName() {
    return "Resolver";
  }
}
