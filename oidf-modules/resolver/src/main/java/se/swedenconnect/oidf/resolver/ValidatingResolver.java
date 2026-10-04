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
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.resolver.chain.ChainValidationError;
import se.swedenconnect.oidf.resolver.chain.ChainValidationResult;
import se.swedenconnect.oidf.resolver.chain.ChainValidator;
import se.swedenconnect.oidf.resolver.metadata.MetadataProcessor;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;
import se.swedenconnect.oidf.resolver.trustmark.TrustMarkCollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
    final List<ChainValidationError> typedErrors = Optional.ofNullable(resolverResponse.typedValidationErrors())
        .orElse(List.of());
    if (!typedErrors.isEmpty()) {
      typedErrors.forEach(error -> explanation.put(counter.getAndIncrement(), Map.of(
          "type", error.getErrorType().name(),
          "message", error.getMessage()
      )));
    } else {
      Optional.ofNullable(resolverResponse.validationErrors())
          .ifPresent(validationErrors -> validationErrors.forEach(error ->
              explanation.put(counter.getAndIncrement(),
                  Map.of(error.getClass().getCanonicalName(), error.getMessage()))
          ));
    }
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
        chain = this.tree.getTrustChain(request);
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
      JSONObject selfMetadata = null;
      try {
        selfMetadata = filterTypes(this.processor.processMetadata(selfChain), request, validationErrors);
      } catch (final Exception e) {
        log.debug("Could not process metadata for self resolved entity:{}", request.subject(), e);
      }
      return ResolverResponse.builder()
          .entityStatement(selfChain.getFirst())
          .metadata(selfMetadata)
          .trustChain(selfChain)
          .validationErrors(validationErrors)
          .build();
    }


    final ResolverTrustChain chain;
    try {
      chain = this.tree.getTrustChainViaAuthorityHints(request)
          .orElseGet(() -> this.tree.getTrustChain(request));
    }
    catch (final RuntimeException e) {
      return this.trustChainError(request, e, validationErrors);
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
   * Builds the response for a trust chain that could not be built from the resolver tree.
   *
   * @param request the resolve request
   * @param e the error from the tree
   * @param validationErrors the validation errors of the request, the trust chain error is added to them
   * @return response holding the validation errors
   */
  private ResolverResponse trustChainError(final ResolveRequest request, final RuntimeException e,
      final List<Exception> validationErrors) {
    log.info("Failed to build trust chain for '{}' to '{}': {}", request.subject(), request.trustAnchor(),
        e.getMessage());
    validationErrors.add(new InvalidTrustChainException(
        "Failed to build trust chain for %s".formatted(request.subject()), e));
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
      issuerChain = this.tree.getTrustChainViaAuthorityHints(request)
          .orElseGet(() -> this.tree.getTrustChain(request))
          .getTrustChain().stream().toList();
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

  @Override
  public DiscoveryResponse discovery(final DiscoveryRequest request) {
    return new DiscoveryResponse(this.tree.discovery(request));
  }

  @Override
  public EntityID getEntityId() {
    return new EntityID(this.resolverProperties.getEntityIdentifier());
  }
}
