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
package se.swedenconnect.oidf.service.resolver;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.utility.DockerImageName;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.resolver.DiscoveryRequest;
import se.swedenconnect.oidf.resolver.DiscoveryResponse;
import se.swedenconnect.oidf.resolver.FederationClientMockFactory;
import se.swedenconnect.oidf.resolver.ResolverResponseFactory;
import se.swedenconnect.oidf.resolver.TestEntitiesFactory;
import se.swedenconnect.oidf.resolver.ValidatingResolver;
import se.swedenconnect.oidf.resolver.chain.ChainValidator;
import se.swedenconnect.oidf.resolver.chain.SignatureValidationStep;
import se.swedenconnect.oidf.resolver.metadata.MetadataProcessor;
import se.swedenconnect.oidf.resolver.metadata.OIDFPolicyOperationFactory;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTreeLoader;
import se.swedenconnect.oidf.resolver.tree.resolution.AtomicIntegerErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.DFSExecution;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContextFactory;
import se.swedenconnect.oidf.service.cache.ResolverEntitySerializer;
import se.swedenconnect.oidf.service.resolver.cache.RedisVersionedCacheLayer;
import se.swedenconnect.oidf.service.resolver.cache.ResolverRedisOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.LEAF_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.SAML_SP_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.TA_ID;

@ExtendWith(MockitoExtension.class)
class ValidatingResolverRedisTreeTest {

  private static final RedisContainer redis =
      new RedisContainer(DockerImageName.parse("redis/redis-stack:latest"));

  private static LettuceConnectionFactory connectionFactory;

  @Mock
  ResolverResponseFactory factory;

  ValidatingResolver resolver;

  @BeforeAll
  static void startRedis() {
    redis.start();
    final RedisStandaloneConfiguration config =
        new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379));
    connectionFactory = new LettuceConnectionFactory(config);
    connectionFactory.afterPropertiesSet();
  }

  @AfterAll
  static void stopRedis() {
    connectionFactory.destroy();
    redis.stop();
  }

  @BeforeEach
  void setUp() throws Exception {
    try (final var conn = connectionFactory.getConnection()) {
      conn.serverCommands().flushAll();
    }

    final StringRedisSerializer stringSerializer = new StringRedisSerializer();

    final RedisTemplate<String, Long> versionTemplate = new RedisTemplate<>();
    versionTemplate.setConnectionFactory(connectionFactory);
    versionTemplate.setKeySerializer(stringSerializer);
    versionTemplate.afterPropertiesSet();

    final RedisTemplate<String, ScrapedEntity> entityTemplate = new RedisTemplate<>();
    entityTemplate.setConnectionFactory(connectionFactory);
    entityTemplate.setKeySerializer(stringSerializer);
    entityTemplate.setHashKeySerializer(stringSerializer);
    entityTemplate.setHashValueSerializer(new ResolverEntitySerializer());
    entityTemplate.afterPropertiesSet();

    final RedisTemplate<String, String> stringTemplate = new RedisTemplate<>();
    stringTemplate.setConnectionFactory(connectionFactory);
    stringTemplate.setKeySerializer(stringSerializer);
    stringTemplate.afterPropertiesSet();

    final TestEntitiesFactory entities = new TestEntitiesFactory();
    final FederationClient client = FederationClientMockFactory.create(entities);

    final ResolverProperties props = new ResolverProperties(
        TA_ID,
        Duration.ofDays(7),
        new JWKSet(entities.taKey.toPublicJWK()),
        "https://resolver.example.com",
        Duration.ofSeconds(5)
    );

    final ResolverRedisOperations operations = new ResolverRedisOperations(
        entityTemplate, stringTemplate, Duration.ofMinutes(10));

    final RedisVersionedCacheLayer cache = new RedisVersionedCacheLayer(
        versionTemplate, operations, props);

    final Tree<ScrapedEntity> tree = new Tree<>(cache);

    final ErrorContextFactory errorContextFactory = new ErrorContextFactory() {
      @Override
      public ErrorContext create(final NodeKey key, final EntityStatementTreeLoader.StepName stepName) {
        return new AtomicIntegerErrorContext();
      }

      @Override
      public ErrorContext createEmpty() {
        return new AtomicIntegerErrorContext();
      }
    };

    final EntityStatementTreeLoader loader = new EntityStatementTreeLoader(
        client, new DFSExecution(), error -> { throw error; }, errorContextFactory);

    loader.resolveTree(TA_ID, tree, 1L);
    cache.useNextVersion();

    final EntityStatementTree entityStatementTree = new EntityStatementTree(tree);

    final ChainValidator validator = new ChainValidator(
        List.of(new SignatureValidationStep(new JWKSet(entities.taKey.toPublicJWK()))));

    final MetadataProcessor processor = new MetadataProcessor(
        new OIDFPolicyOperationFactory(), new DefaultPolicyOperationCombinationValidator());

    resolver = new ValidatingResolver(props, validator, entityStatementTree, processor, factory);
  }

  @Test
  void resolveLeafReturnsSignedResponse() throws Exception {
    when(factory.sign(any())).thenReturn("mock-signed-resolve-response");

    final ResolveRequest request = new ResolveRequest(LEAF_ID, TA_ID, "openid_relying_party", false);
    assertNotNull(resolver.resolve(request));
  }

  @Test
  void discoveryReturnsLeafEntityId() {
    final DiscoveryRequest request = new DiscoveryRequest(TA_ID, List.of("openid_relying_party"), null);
    final DiscoveryResponse response = resolver.discovery(request);

    assertFalse(response.supportedEntities().isEmpty());
    assert response.supportedEntities().contains(LEAF_ID);
  }

  @Test
  void wrongTrustAnchorThrowsFederationException() {
    final ResolveRequest request = new ResolveRequest(LEAF_ID, "https://other-ta.example.com", "openid_relying_party", false);
    assertThrows(Exception.class, () -> resolver.resolve(request));
  }

  @Test
  void missingSubjectThrowsFederationException() {
    final ResolveRequest request = new ResolveRequest("https://unknown.example.com", TA_ID, "openid_relying_party", false);
    assertThrows(Exception.class, () -> resolver.resolve(request));
  }

  @Test
  void resolveSamlSpReturnsSignedResponse() throws Exception {
    when(factory.sign(any())).thenReturn("mock-signed-resolve-response");

    final ResolveRequest request = new ResolveRequest(SAML_SP_ID, TA_ID, "saml_service_provider", false);
    assertNotNull(resolver.resolve(request));
  }

  @Test
  void discoverySamlSpByType() {
    final DiscoveryRequest request = new DiscoveryRequest(TA_ID, List.of("saml_service_provider"), null);
    final DiscoveryResponse response = resolver.discovery(request);

    assertFalse(response.supportedEntities().isEmpty());
    assert response.supportedEntities().contains(SAML_SP_ID);
    assert !response.supportedEntities().contains(LEAF_ID);
  }
}
