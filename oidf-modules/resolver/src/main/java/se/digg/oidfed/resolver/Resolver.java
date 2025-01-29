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
import se.digg.oidfed.common.module.Submodule;
import se.digg.oidfed.resolver.chain.ChainValidationResult;
import se.digg.oidfed.resolver.chain.ChainValidator;
import se.digg.oidfed.resolver.metadata.MetadataProcessor;
import se.digg.oidfed.resolver.tree.EntityStatementTree;
import se.digg.oidfed.resolver.trustmark.TrustMarkCollector;

import java.util.List;
import java.util.Set;

/**
 * Resolver implementation.
 *
 * @author Felix Hellman
 */
public class Resolver implements Submodule {

  private final ResolverProperties resolverProperties;

  private final ChainValidator validator;

  private final EntityStatementTree tree;

  private final MetadataProcessor processor;

  private final ResolverResponseFactory factory;

  /**
   * Constructor.
   * @param resolverProperties from configuration
   * @param validator for validating trust chains
   * @param tree data structure to search upon
   * @param processor for processing metadata
   * @param factory to create signed responses
   */
  public Resolver(final ResolverProperties resolverProperties, final ChainValidator validator,
      final EntityStatementTree tree,
      final MetadataProcessor processor, final ResolverResponseFactory factory) {
    this.resolverProperties = resolverProperties;
    this.validator = validator;
    this.tree = tree;
    this.processor = processor;
    this.factory = factory;
  }

  /**
   * @param request from the resolver api
   * @return response
   * @throws JOSEException
   */
  public String resolve(final ResolverRequest request) {

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

    if (!request.trustAnchor().equalsIgnoreCase(this.resolverProperties.trustAnchor())) {
      throw new IllegalArgumentException("Requested Trust Anchor is not supported");
    }

    final Set<EntityStatement> chain = this.tree.getTrustChain(request);
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
      return this.factory.sign(response);
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

  @Override
  public String getAlias() {
    return this.resolverProperties.alias();
  }

  @Override
  public List<EntityID> getEntityIds() {
    return List.of();
  }
}
