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

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import se.digg.oidfed.common.entity.integration.federation.EntityConfigurationRequest;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.entity.integration.federation.FederationRequest;
import se.digg.oidfed.common.entity.integration.federation.FetchRequest;
import se.digg.oidfed.common.entity.integration.federation.ResolveRequest;
import se.digg.oidfed.common.entity.integration.federation.SubordinateListingRequest;
import se.digg.oidfed.common.entity.integration.registry.ResolverProperties;
import se.digg.oidfed.common.jwt.SignerFactory;
import se.digg.oidfed.common.tree.ResolverCache;
import se.digg.oidfed.common.tree.Tree;
import se.digg.oidfed.common.tree.VersionedInMemoryCache;
import se.digg.oidfed.resolver.chain.ChainValidator;
import se.digg.oidfed.resolver.chain.ConstraintsValidationStep;
import se.digg.oidfed.resolver.chain.CriticalClaimsValidationStep;
import se.digg.oidfed.resolver.chain.SignatureValidationStep;
import se.digg.oidfed.resolver.metadata.MetadataProcessor;
import se.digg.oidfed.resolver.metadata.OIDFPolicyOperationFactory;
import se.digg.oidfed.resolver.tree.EntityStatementTree;
import se.digg.oidfed.resolver.tree.EntityStatementTreeLoader;
import se.digg.oidfed.resolver.tree.resolution.DFSExecution;
import se.digg.oidfed.resolver.tree.resolution.DefaultErrorContextFactory;
import se.digg.oidfed.resolver.tree.resolution.DeferredStepRecoveryStrategy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

class ResolverTest {

  private static JWK KEY;

  static {
    try {
      ResolverTest.KEY = new RSAKeyGenerator(2048)
          .keyUse(KeyUse.SIGNATURE)
          .keyID(UUID.randomUUID().toString())
          .issueTime(new Date())
          .generate();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testResolveFederation() throws Exception {
    final FederationClient mock = Mockito.mock(FederationClient.class);

    final EntityStatement trustAnchor = createEntityStatement("https://start.test", "https://start.test",
        c -> {
          c.put("federation_list_endpoint", "https://start.test/list");
          c.put("federation_fetch_endpoint", "https://start.test/fetch");
          return c;
        });

    final EntityStatement secondEntity = createEntityStatement(
        "https://second.test",
        "https://second.test",
        c -> c);


    mockEntityStatement(mock, trustAnchor);
    mockEntityStatement(mock, secondEntity);
    mockSubjectStatement(mock, createEntityStatement(trustAnchor.getEntityID().getValue(),
        secondEntity.getEntityID().getValue(), c -> c));
    mockSubordinateListing(mock, "https://start.test/list", List.of("https://second.test"));


    final EntityStatementTreeLoader entityStatementTreeLoader = new EntityStatementTreeLoader(mock, new DFSExecution(), new DeferredStepRecoveryStrategy(),
        new DefaultErrorContextFactory());

    final ResolverCache dataLayer = new VersionedInMemoryCache();
    final Tree<EntityStatement> entityStatementTree =
        new Tree<>(dataLayer);
    entityStatementTreeLoader.withAdditionalPostHook(dataLayer::useNextVersion);
    entityStatementTreeLoader.resolveTree("https://start.test", entityStatementTree);

    final ResolverProperties properties = new ResolverProperties("https://start.test", Duration.ofDays(7), List.of(KEY), "https://resolver" +
        ".test", Duration.ofSeconds(10), "resolve");
    final String response = createResolver(properties, entityStatementTree).resolve(new ResolveRequest("https://second.test", "https://start" +
        ".test", null));


    Assertions.assertTrue(SignedJWT.parse(response).verify(new RSASSAVerifier(KEY.toRSAKey())));
  }

  private static Resolver createResolver(final ResolverProperties properties,
                                         final Tree<EntityStatement> entityStatementTree) {
    return new Resolver(properties,
        new ChainValidator(List.of(
            new SignatureValidationStep(new JWKSet(List.of(KEY))),
            new CriticalClaimsValidationStep(),
            new ConstraintsValidationStep())
        ),
        new EntityStatementTree(entityStatementTree),
        new MetadataProcessor(new OIDFPolicyOperationFactory(), new DefaultPolicyOperationCombinationValidator()),
        new ResolverResponseFactory(Clock.systemUTC(), properties, new SignerFactory(new JWKSet(List.of(KEY)))));
  }

  private static void mockSubordinateListing(final FederationClient mock, final String listEndpoint, final List<String> subordinates) {
    final FederationRequest<SubordinateListingRequest> rootSubordinates = ArgumentMatchers.argThat(request -> {
      return request.federationEntityMetadata().containsKey("federation_list_endpoint") && request.federationEntityMetadata().get("federation_list_endpoint").equals(listEndpoint);
    });

    Mockito.when(mock.subordinateListing(rootSubordinates))
        .thenReturn(subordinates);
  }

  private static void mockSubjectStatement(final FederationClient mock,
                                           final EntityStatement entityConfiguration) {
    final FederationRequest<FetchRequest> matcher = ArgumentMatchers.argThat(request -> {
      return request != null && request.parameters().subject().equals(entityConfiguration.getClaimsSet().getSubject().getValue());
    });
    Mockito.when(mock.fetch(matcher)).thenReturn(entityConfiguration);
  }

  private static void mockEntityStatement(final FederationClient mock,
                                          final EntityStatement entityConfiguration) {
    final FederationRequest<EntityConfigurationRequest> matcher = ArgumentMatchers.argThat(request -> {
      return request != null && request.parameters().entityID().equals(entityConfiguration.getEntityID());
    });
    Mockito.when(mock.entityConfiguration(matcher)).thenReturn(entityConfiguration);
  }

  private static EntityStatement createEntityStatement(
      final String issuer,
      final String subject,
      final Function<JSONObject, JSONObject> customizer) {
    try {
      final EntityStatementClaimsSet claims = new EntityStatementClaimsSet(
          new Issuer(issuer),
          new Subject(subject),
          Date.from(Instant.now()),
          Date.from(Instant.now().plus(7, ChronoUnit.DAYS)),
          new JWKSet(List.of(KEY))
      );

      final JSONObject metadata = new JSONObject();
      metadata.put("organization_name", "orgname");
      claims.setMetadata(EntityType.FEDERATION_ENTITY, customizer.apply(metadata));

      return EntityStatement.sign(claims, KEY);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}