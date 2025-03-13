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
package se.digg.oidfed.resolver;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import net.minidev.json.JSONObject;
import se.digg.oidfed.common.entity.integration.Cache;
import se.digg.oidfed.common.entity.integration.Expirable;
import se.digg.oidfed.common.entity.integration.federation.ResolveRequest;
import se.digg.oidfed.common.entity.integration.registry.ResolverProperties;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.common.exception.InvalidTrustAnchorException;
import se.digg.oidfed.common.exception.NotFoundException;
import se.digg.oidfed.resolver.chain.ChainValidationResult;
import se.digg.oidfed.resolver.chain.ChainValidator;
import se.digg.oidfed.resolver.metadata.MetadataProcessor;
import se.digg.oidfed.resolver.tree.EntityStatementTree;
import se.digg.oidfed.resolver.trustmark.TrustMarkCollector;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Resolver implementation.
 *
 * @author Felix Hellman
 */
public class Resolver {

  private final ResolverProperties resolverProperties;

  private final ChainValidator validator;

  private final EntityStatementTree tree;

  private final MetadataProcessor processor;

  private final ResolverResponseFactory factory;

  private final Cache<String, String> cache;

  private final Clock clock;

  /**
   * Constructor.
   *
   * @param resolverProperties from configuration
   * @param validator          for validating trust chains
   * @param tree               data structure to search upon
   * @param processor          for processing metadata
   * @param factory            to create signed responses
   * @param cache              for handling repeat requests
   * @param clock              for time keeping
   */
  public Resolver(final ResolverProperties resolverProperties,
                  final ChainValidator validator,
                  final EntityStatementTree tree,
                  final MetadataProcessor processor,
                  final ResolverResponseFactory factory,
                  final Cache<String, String> cache,
                  final Clock clock) {

    this.resolverProperties = resolverProperties;
    this.validator = validator;
    this.tree = tree;
    this.processor = processor;
    this.factory = factory;
    this.cache = cache;
    this.clock = clock;
  }

  /**
   * @param request from the resolver api
   * @return response
   * @throws JOSEException
   */
  public String resolve(final ResolveRequest request) throws FederationException {

    /*
     * 1) resolve the chain
     * 2) Validate the chain
     * 3) Get Trust Marks
     * 4) For each trust Mark
     * 4 a) resolve the Trust Mark chain
     * 4 b) validate the chain
     * 4 c) validate the trust mark (supported by trust anchor, validity) or renew it if necessary
     * 5) Filter metadata according to type
     * 6) Build response
     */

    final String cachedValue = this.cache.get(request.toKey());
    if (Objects.nonNull(cachedValue) && !cachedValue.isBlank()) {
      return cachedValue;
    }

    if (!request.trustAnchor().equalsIgnoreCase(this.resolverProperties.trustAnchor())) {
      throw new InvalidTrustAnchorException("The Trust Anchor cannot be found or used.");
    }

    final Set<EntityStatement> chain = this.tree.getTrustChain(request);
    if (chain.isEmpty()) {
      throw new NotFoundException("Resolver found no subject with requested EntityID:%s".formatted(request.subject()));
    }
    final ChainValidationResult chainValidationResult = this.validator.validate(chain.stream().toList());

    final JSONObject processedMetadata = this.processor.processMetadata(chainValidationResult.chain());
    final List<TrustMarkEntry> trustMarkEntries =
        TrustMarkCollector.collectSubjectTrustMarks(chainValidationResult.chain());

    final EntityStatement leaf = chainValidationResult.chain().getFirst();

    final ResolverResponse response = ResolverResponse.builder()
        .entityStatement(leaf)
        .metadata(processedMetadata)
        .trustMarkEntries(trustMarkEntries)
        .trustChain(chainValidationResult.chain())
        .build();

    try {
      final String sign = this.factory.sign(response);
      this.cache.add(request.toKey(),
          new Expirable<>(
              Instant.now().plus(1, ChronoUnit.MINUTES),
              Instant.now(this.clock), sign)
      );
      return sign;
    } catch (final JOSEException | ParseException e) {
      throw new ResolverException("Failed to sign resolver response", e);
    }
  }

  /**
   * @param request to process
   * @return discovery response
   */
  public DiscoveryResponse discovery(final DiscoveryRequest request) {
    return new DiscoveryResponse(this.tree.discovery(request));
  }

  /**
   * @return entity id of this resolver
   */
  public EntityID getEntityId() {
    return new EntityID(this.resolverProperties.entityIdentifier());
  }
}
