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
package se.swedenconnect.oidf.resolver;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.exception.InvalidTrustAnchorException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.resolver.chain.ChainValidationResult;
import se.swedenconnect.oidf.resolver.chain.ChainValidator;
import se.swedenconnect.oidf.resolver.metadata.MetadataProcessor;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;
import se.swedenconnect.oidf.resolver.trustmark.TrustMarkCollector;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resolver implementation.
 *
 * @author Felix Hellman
 */
public class ValidatingResolver implements Resolver {

  private final ResolverProperties resolverProperties;

  private final ChainValidator validator;

  private final EntityStatementTree tree;

  private final MetadataProcessor processor;

  private final ResolverResponseFactory factory;

  /**
   * Constructor.
   *
   * @param resolverProperties from configuration
   * @param validator          for validating trust chains
   * @param tree               data structure to search upon
   * @param processor          for processing metadata
   * @param factory            to create signed responses
   */
  public ValidatingResolver(final ResolverProperties resolverProperties,
                            final ChainValidator validator,
                            final EntityStatementTree tree,
                            final MetadataProcessor processor,
                            final ResolverResponseFactory factory) {

    this.resolverProperties = resolverProperties;
    this.validator = validator;
    this.tree = tree;
    this.processor = processor;
    this.factory = factory;
  }

  @Override
  public Map<Integer, Map<String, String>> explain(final ResolveRequest request) {
    final HashMap<Integer, Map<String, String>> explanation = new HashMap<>();
    final AtomicInteger counter = new AtomicInteger();
    final ResolverResponse resolverResponse = this.internalResolve(request);
    Optional.ofNullable(resolverResponse.validationErrors())
            .ifPresent(validationErrors -> {
              validationErrors
                  .forEach(error -> {
                    explanation.put(
                        counter.getAndIncrement(),
                        Map.of(error.getClass().getCanonicalName(), error.getMessage())
                    );
                  });
            });
    return explanation;
  }

  @Override
  public String resolve(final ResolveRequest request) throws FederationException {

    final ResolverResponse response = this.internalResolve(request);

    if (!response.validationErrors().isEmpty()) {
      final Exception exception = response.validationErrors().getFirst();
      if (exception instanceof FederationException federationException) {
        throw federationException;
      }
      throw new FederationException("Validation failed with %d errors".formatted(response.validationErrors().size()),
          exception.getMessage(),
          exception);
    }

    try {
      return this.factory.sign(response);
    } catch (final JOSEException | ParseException e) {
      throw new ResolverException("Failed to sign resolver response", e);
    }
  }

  private ResolverResponse internalResolve(final ResolveRequest request) {
    final List<Exception> validationErrors = new ArrayList<>();

    if (!request.trustAnchor().equalsIgnoreCase(this.resolverProperties.getTrustAnchor())) {
      validationErrors.add(new InvalidTrustAnchorException("The Trust Anchor cannot be found or used."));
    }

    if (request.trustAnchor().equals(request.subject())) {
      final ResolverTrustChain chain = this.tree.getTrustChain(request);
      try {
        return ResolverResponse.builder()
            .entityStatement(EntityStatement.parse(chain.getTrustChain().stream().findFirst().get().getSignedStatement()))
            .build();
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    }


    final ResolverTrustChain chain = this.tree.getTrustChain(request);
    if (chain.getTrustChain().isEmpty()) {
      validationErrors.add(
          new NotFoundException("Resolver found no subject with requested EntityID:%s".formatted(request.subject()))
      );
    }
    ChainValidationResult chainValidationResult = null;
    final List<EntityStatement> trustChainList = chain.getTrustChain().stream().toList();
    try {
      chainValidationResult = this.validator.validate(trustChainList);
      validationErrors.addAll(chainValidationResult.errors());
    } catch (final Exception e) {
      validationErrors.add(e);
    }


    JSONObject processedMetadata = null;
    try {
      processedMetadata = this.processor.processMetadata(trustChainList);
    } catch (final Exception e) {
      validationErrors.add(e);
    }
    List<TrustMarkEntry> trustMarkEntries = null;
    try {
      trustMarkEntries = TrustMarkCollector.collectSubjectTrustMarks(chain);
    } catch (final Exception e) {
      validationErrors.add(e);
    }

    final EntityStatement leaf = chainValidationResult.chain().getFirst();

    return ResolverResponse.builder()
        .entityStatement(leaf)
        .metadata(processedMetadata)
        .trustMarkEntries(trustMarkEntries)
        .trustChain(trustChainList)
        .validationErrors(validationErrors)
        .build();
  }

  @Override
  public DiscoveryResponse discovery(final DiscoveryRequest request) {
    return new DiscoveryResponse(this.tree.discovery(request));
  }

  @Override
  public EntityID getEntityId() {
    return new EntityID(this.resolverProperties.getEntityIdentifier());
  }
}
