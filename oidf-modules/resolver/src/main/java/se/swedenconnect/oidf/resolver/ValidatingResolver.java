/*
 * Copyright 2024-2026 Sweden Connect
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
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.exception.InvalidTrustAnchorException;
import se.swedenconnect.oidf.common.entity.exception.InvalidTrustChainException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.exception.ServerErrorException;
import se.swedenconnect.oidf.common.entity.exception.TemporarilyUnavailableException;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.common.entity.tree.TreeNotLoadedException;
import se.swedenconnect.oidf.resolver.chain.ChainValidationError;
import se.swedenconnect.oidf.resolver.chain.ChainValidationFailedException;
import se.swedenconnect.oidf.resolver.chain.ChainValidationResult;
import se.swedenconnect.oidf.resolver.chain.ChainValidator;
import se.swedenconnect.oidf.resolver.metadata.MetadataProcessor;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;
import se.swedenconnect.oidf.resolver.tree.TreeTrustChainSource;
import se.swedenconnect.oidf.resolver.tree.TrustChainSource;
import se.swedenconnect.oidf.resolver.trustmark.TrustMarkCollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Resolver implementation.
 *
 * @author Felix Hellman
 */
@Slf4j
public class ValidatingResolver implements Resolver {

  private final ResolverProperties resolverProperties;

  private final ChainValidator validator;

  private final EntityStatementTree tree;

  private final MetadataProcessor processor;

  private final ResolverResponseFactory factory;
  private final TrustChainSource source;

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

    this(resolverProperties, validator, tree, processor, factory, new TreeTrustChainSource(tree));
  }

  /**
   * Constructor.
   *
   * @param resolverProperties for this resolver
   * @param validator          for trust chains
   * @param tree               for discovery
   * @param processor          for metadata
   * @param factory            for responses
   * @param source             of the trust chains to validate
   */
  public ValidatingResolver(final ResolverProperties resolverProperties,
                            final ChainValidator validator,
                            final EntityStatementTree tree,
                            final MetadataProcessor processor,
                            final ResolverResponseFactory factory,
                            final TrustChainSource source) {

    this.resolverProperties = resolverProperties;
    this.validator = validator;
    this.tree = tree;
    this.processor = processor;
    this.factory = factory;
    this.source = source;
  }

  /**
   * Explains why a resolve request fails, listing every error found. All checks are executed, also when the trust
   * chain could not be fully built, so that all reasons are reported and not only the first one. Errors from the
   * chain validation are listed with their type, other errors with the class of the error.
   *
   * @param request the resolve request
   * @return the errors, in the order they were found
   */
  @Override
  public Map<Integer, Map<String, String>> explain(final ResolveRequest request) {
    final Map<Integer, Map<String, String>> explanation = new LinkedHashMap<>();
    final AtomicInteger counter = new AtomicInteger();
    final ResolverResponse resolverResponse = this.internalResolve(request);
    final List<ChainValidationError> typedErrors = resolverResponse.typedValidationErrors();
    final Runnable explainTypedErrors = () -> typedErrors.forEach(error ->
        explanation.put(counter.getAndIncrement(), Map.of(
            "type", error.getErrorType().name(),
            "message", describe(error)
        )));
    boolean typedErrorsExplained = false;
    for (final Exception error : resolverResponse.validationErrors()) {
      if (error instanceof ChainValidationFailedException) {
        // Summary of the typed errors, which are listed one by one instead
        if (!typedErrorsExplained) {
          explainTypedErrors.run();
          typedErrorsExplained = true;
        }
        continue;
      }
      explanation.put(counter.getAndIncrement(), Map.of(error.getClass().getCanonicalName(), describe(error)));
    }
    if (!typedErrorsExplained) {
      explainTypedErrors.run();
    }
    return explanation;
  }

  /**
   * Describes an error together with its causes. A cause whose message is already part of the description, as when
   * an error message includes the message of its cause, is left out so that it is not repeated.
   *
   * @param error the error to describe
   * @return the messages of the error and its causes
   */
  private static String describe(final Throwable error) {
    final StringBuilder description = new StringBuilder();
    Stream.iterate(error, Objects::nonNull, Throwable::getCause)
        .limit(10)
        .map(cause -> Optional.ofNullable(cause.getMessage()).orElse(cause.getClass().getSimpleName()))
        .filter(message -> !description.toString().contains(message))
        .forEach(message -> description.append(description.isEmpty() ? "" : ": ").append(message));
    return description.toString();
  }

  @Override
  public String resolve(final ResolveRequest request) throws FederationException {

    final ResolverResponse response = this.internalResolve(request);

    if (!response.validationErrors().isEmpty()) {
      final Exception exception = response.validationErrors().getFirst();
      if (exception instanceof FederationException federationException) {
        throw federationException;
      }
      throw new ServerErrorException("Validation failed with %d errors: %s"
          .formatted(response.validationErrors().size(), exception.getMessage()), exception);
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
      final ResolverTrustChain chain;
      try {
        chain = this.source.trustChain(request);
      }
      catch (final RuntimeException e) {
        return this.trustChainError(request, e, validationErrors);
      }
      final List<SignedJWT> selfChain = chain.getTrustChain().stream().toList();
      if (selfChain.isEmpty()) {
        validationErrors.add(
            new NotFoundException("Resolver found no subject with requested EntityID:%s".formatted(request.subject()))
        );
        return ResolverResponse.builder()
            .validationErrors(validationErrors)
            .build();
      }
      // The Trust Anchor Entity Configuration is validated like any other trust chain (Sections 8.3 and 10.2)
      final ChainValidationResult selfValidation = this.validator.validateTrustAnchor(selfChain.getFirst());
      validationErrors.addAll(selfValidation.errors());
      JSONObject selfMetadata = null;
      try {
        selfMetadata = filterTypes(this.processor.processMetadata(selfChain), request, validationErrors);
      } catch (final Exception e) {
        validationErrors.add(e);
      }
      return ResolverResponse.builder()
          .entityStatement(selfChain.getFirst())
          .metadata(selfMetadata)
          .trustChain(selfChain)
          .validationErrors(validationErrors)
          .typedValidationErrors(selfValidation.typedErrors())
          .build();
    }


    final ResolverTrustChain chain;
    try {
      chain = this.source.trustChain(request);
    }
    catch (final RuntimeException e) {
      return this.trustChainError(request, e, validationErrors);
    }
    // Links that could not be built are reported, the rest of the chain is still validated to find all problems
    chain.getBuildErrors().forEach(error -> validationErrors.add(new InvalidTrustChainException(
        "Failed to build trust chain for %s".formatted(request.subject()), new IllegalStateException(error))));
    if (!chain.isComplete()) {
      log.info("Failed to build trust chain for '{}' to '{}': {}", request.subject(), request.trustAnchor(),
          chain.getBuildErrors());
    }
    if (chain.getTrustChain().isEmpty()) {
      validationErrors.add(
          new NotFoundException("Resolver found no subject with requested EntityID:%s".formatted(request.subject()))
      );
    }
    ChainValidationResult chainValidationResult = null;
    final List<SignedJWT> trustChainList = chain.getTrustChain().stream().toList();
    try {
      chainValidationResult = this.validator.validate(trustChainList);
      validationErrors.addAll(chainValidationResult.errors());
    } catch (final Exception e) {
      validationErrors.add(e);
    }


    JSONObject processedMetadata = null;
    try {
      processedMetadata = filterTypes(this.processor.processMetadata(trustChainList), request, validationErrors);
    } catch (final Exception e) {
      validationErrors.add(e);
    }
    List<TrustMarkEntry> trustMarkEntries = null;
    try {
      trustMarkEntries = TrustMarkCollector.collectSubjectTrustMarks(chain,
          issuer -> this.resolveTrustMarkIssuerKeys(issuer, request.trustAnchor()));
    } catch (final Exception e) {
      validationErrors.add(e);
    }

    if (chainValidationResult == null || chainValidationResult.chain().isEmpty()) {
      return ResolverResponse.builder()
          .metadata(processedMetadata)
          .trustMarkEntries(trustMarkEntries)
          .trustChain(trustChainList)
          .validationErrors(validationErrors)
          .build();
    }

    final SignedJWT leaf = chainValidationResult.chain().getFirst();

    return ResolverResponse.builder()
        .entityStatement(leaf)
        .metadata(processedMetadata)
        .trustMarkEntries(trustMarkEntries)
        .trustChain(trustChainList)
        .validationErrors(validationErrors)
        .typedValidationErrors(chainValidationResult.typedErrors())
        .build();
  }

  /**
   * Builds the response for a trust chain that could not be built from the resolver tree. A tree that has not
   * been loaded is reported as temporarily unavailable, since the request can be answered once a load has
   * succeeded.
   *
   * @param request the resolve request
   * @param e the error from the tree
   * @param validationErrors the validation errors of the request, the trust chain error is added to them
   * @return response holding the validation errors
   */
  private ResolverResponse trustChainError(final ResolveRequest request, final RuntimeException e,
      final List<Exception> validationErrors) {
    if (e instanceof TreeNotLoadedException) {
      log.warn("Cannot resolve '{}' to '{}', the resolver has no loaded tree", request.subject(),
          request.trustAnchor());
      validationErrors.add(new TemporarilyUnavailableException(
          "The resolver has no loaded tree for %s".formatted(request.trustAnchor()), e));
    } else {
      log.info("Failed to build trust chain for '{}' to '{}': {}", request.subject(), request.trustAnchor(),
          e.getMessage());
      validationErrors.add(new InvalidTrustChainException(
          "Failed to build trust chain for %s".formatted(request.subject()), e));
    }
    return ResolverResponse.builder()
        .validationErrors(validationErrors)
        .build();
  }

  /**
   * Keeps only the entity types requested with {@code entity_type} (OpenID Federation 1.0, Section 8.3.1). When the
   * subject has none of the requested types, a {@link NotFoundException} is added to the validation errors.
   *
   * @param metadata the resolved metadata
   * @param request the resolve request
   * @param validationErrors the validation errors of the request
   * @return the metadata of the requested types, or all metadata if no types were requested
   */
  private static JSONObject filterTypes(final JSONObject metadata, final ResolveRequest request,
      final List<Exception> validationErrors) {
    if (metadata == null || request.types() == null || request.types().isEmpty()) {
      return metadata;
    }
    final JSONObject filtered = new JSONObject();
    request.types().stream().filter(metadata::containsKey).forEach(type -> filtered.put(type, metadata.get(type)));
    if (filtered.isEmpty()) {
      validationErrors.add(new NotFoundException("Subject %s has none of the requested entity types %s"
          .formatted(request.subject(), request.types())));
    }
    return filtered;
  }

  /**
   * Resolves the federation entity keys of a trust mark issuer through a valid trust chain to the trust anchor.
   *
   * @param issuer the trust mark issuer
   * @param trustAnchor the trust anchor of the resolve request
   * @return the keys of the issuer, or empty if no valid trust chain was found
   */
  private Optional<JWKSet> resolveTrustMarkIssuerKeys(final String issuer, final String trustAnchor) {
    final ResolveRequest request = new ResolveRequest(issuer, trustAnchor, null, false);
    final List<SignedJWT> issuerChain;
    try {
      final ResolverTrustChain chain = this.source.trustChain(request);
      if (!chain.isComplete()) {
        log.debug("Failed to build trust chain for trust mark issuer '{}': {}", issuer, chain.getBuildErrors());
        return Optional.empty();
      }
      issuerChain = chain.getTrustChain().stream().toList();
    }
    catch (final RuntimeException e) {
      log.debug("Failed to build trust chain for trust mark issuer '{}': {}", issuer, e.getMessage());
      return Optional.empty();
    }
    if (issuerChain.isEmpty()) {
      log.debug("No trust chain found for trust mark issuer '{}'", issuer);
      return Optional.empty();
    }
    if (issuerChain.size() > 1) {
      try {
        final ChainValidationResult result = this.validator.validate(issuerChain);
        if (!result.errors().isEmpty()) {
          log.debug("Trust chain for trust mark issuer '{}' is not valid: {}", issuer, result.errors());
          return Optional.empty();
        }
      }
      catch (final Exception e) {
        log.debug("Trust chain for trust mark issuer '{}' is not valid: {}", issuer, e.getMessage());
        return Optional.empty();
      }
    }
    return Optional.ofNullable(EntityStatementClaims.getJWKSet(issuerChain.getFirst()));
  }

  /**
   * Finds the entities matching a discovery request. With {@code trust_mark_type}, an entity is only included if it
   * has a valid trust mark of one of the requested types, checked as in resolve: the trust mark must pass the checks
   * of OpenID Federation 1.0, Section 7.3, its issuer must be reachable through a valid trust chain, and its status
   * must be active when the issuer publishes a status endpoint.
   *
   * @param request the discovery request
   * @return the matching entities
   * @throws FederationException if the resolver has no loaded tree
   */
  @Override
  public DiscoveryResponse discovery(final DiscoveryRequest request) throws FederationException {
    final List<String> entities;
    try {
      entities = this.tree.discovery(request);
    } catch (final TreeNotLoadedException e) {
      log.warn("Cannot answer discovery for '{}', the resolver has no loaded tree", request.trustAnchor());
      throw new TemporarilyUnavailableException(
          "The resolver has no loaded tree for %s".formatted(request.trustAnchor()), e);
    }
    if (request.trustMarkTypes() == null || request.trustMarkTypes().isEmpty()) {
      return new DiscoveryResponse(entities);
    }
    final Map<String, Optional<JWKSet>> issuerKeys = new HashMap<>();
    final Function<String, Optional<JWKSet>> issuerKeyResolver = issuer -> issuerKeys.computeIfAbsent(issuer,
        i -> this.resolveTrustMarkIssuerKeys(i, request.trustAnchor()));
    return new DiscoveryResponse(entities.stream()
        .filter(entityId -> this.hasValidTrustMark(entityId, request, issuerKeyResolver))
        .toList());
  }

  /**
   * Tells whether an entity has a valid trust mark of one of the requested types.
   *
   * @param entityId the entity
   * @param request the discovery request
   * @param issuerKeyResolver resolves the keys of trust mark issuers
   * @return true if the entity has a valid trust mark of a requested type
   */
  private boolean hasValidTrustMark(final String entityId, final DiscoveryRequest request,
      final Function<String, Optional<JWKSet>> issuerKeyResolver) {
    final ResolveRequest resolveRequest = new ResolveRequest(entityId, request.trustAnchor(), null, false);
    try {
      final ResolverTrustChain chain = this.source.trustChain(resolveRequest);
      if (chain.getTrustChain().isEmpty() || !chain.isComplete()) {
        return false;
      }
      return TrustMarkCollector.collectSubjectTrustMarks(chain, issuerKeyResolver).stream()
          .anyMatch(entry -> request.trustMarkTypes().contains(entry.getID().getValue()));
    }
    catch (final Exception e) {
      log.debug("Discovery leaves out '{}', its trust marks could not be checked: {}", entityId, e.getMessage());
      return false;
    }
  }

  @Override
  public EntityID getEntityId() {
    return new EntityID(this.resolverProperties.getEntityIdentifier());
  }
}
