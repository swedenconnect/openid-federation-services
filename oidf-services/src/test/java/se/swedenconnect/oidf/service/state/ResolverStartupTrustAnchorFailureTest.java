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
package se.swedenconnect.oidf.service.state;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.RestClientFederationClient;
import se.swedenconnect.oidf.UptimeRegistry;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EcLocationValidator;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.common.entity.tree.VersionedInMemoryCache;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTreeLoader;
import se.swedenconnect.oidf.resolver.tree.resolution.AtomicIntegerErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.DFSExecution;
import se.swedenconnect.oidf.service.resolver.cache.CompositeTreeLoader;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the initial resolver load when the trust anchor cannot be fetched, as when it is served through a
 * load balancer that answers 503 while no node is up.
 */
class ResolverStartupTrustAnchorFailureTest {

  private static WireMockServer trustAnchor;

  /** Set when the trust anchor should answer 503 for the load that is about to run. */
  private static final AtomicBoolean TRUST_ANCHOR_UNAVAILABLE = new AtomicBoolean(true);

  /** Manager of the last started application, kept so the test can read its ready state. */
  private static ResolverStateManager startedManager;

  @BeforeEach
  void setUp() {
    trustAnchor = new WireMockServer(0);
    trustAnchor.start();
    WireMock.configureFor(trustAnchor.port());
    WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/trustanchor/.well-known/openid-federation"))
        .willReturn(WireMock.aResponse()
            .withStatus(503)
            .withBody("No server is available to handle this request.")));
    TRUST_ANCHOR_UNAVAILABLE.set(true);
    startedManager = null;
  }

  @AfterEach
  void tearDown() {
    trustAnchor.stop();
  }

  @Test
  void applicationStartsWithoutTheTrustAnchorAndIsNotReady() {
    final SpringApplication application = new SpringApplication(FailingTrustAnchorConfiguration.class);
    application.setWebApplicationType(WebApplicationType.NONE);

    try (final ConfigurableApplicationContext context = application.run()) {
      assertNotNull(context, "The 503 from the trust anchor should not fail the application start");
      assertFalse(startedManager.ready(),
          "A resolver without a loaded tree should not report ready for traffic");
    }
  }

  @Test
  void scheduledReloadRetriesTheFailedInitialLoad() {
    final ResolverStateManager manager = new ResolverStateManager(
        new TrustAnchorTreeLoader(), new AlwaysFreeLock(), ObservationRegistry.NOOP);

    manager.handle(new RegistryReadyEvent());
    assertFalse(manager.ready(), "The failed initial load should leave the resolver not ready");

    TRUST_ANCHOR_UNAVAILABLE.set(false);
    manager.reload();
    assertTrue(manager.ready(), "The resolver should be ready once a load has succeeded");
  }

  @Test
  void failedReloadKeepsAReadyResolverInTraffic() {
    TRUST_ANCHOR_UNAVAILABLE.set(false);
    final ResolverStateManager manager = new ResolverStateManager(
        new TrustAnchorTreeLoader(), new AlwaysFreeLock(), ObservationRegistry.NOOP);
    manager.handle(new RegistryReadyEvent());
    assertTrue(manager.ready());

    TRUST_ANCHOR_UNAVAILABLE.set(true);
    manager.reload();

    assertTrue(manager.ready(),
        "A resolver that still serves the trees of its previous load should stay ready");
  }

  /**
   * Tree loader that resolves its root through a real federation client against the trust anchor, which is
   * either unavailable or - standing in for a successful load - not read at all.
   */
  private static class TrustAnchorTreeLoader extends CompositeTreeLoader {

    TrustAnchorTreeLoader() {
      super(null, null, null);
    }

    @Override
    public void loadTree() {
      if (!TRUST_ANCHOR_UNAVAILABLE.get()) {
        return;
      }
      final RestClientFederationClient client = new RestClientFederationClient(
          RestClient.builder().build(),
          new SimpleMeterRegistry(),
          new UptimeRegistry(),
          new EcLocationValidator(false));
      new EntityStatementTreeLoader(client, new DFSExecution(),
          (key, stepName) -> new AtomicIntegerErrorContext(), new EcLocationValidator(false))
          .resolveTree(trustAnchor.baseUrl() + "/trustanchor",
              new Tree<>(new VersionedInMemoryCache()), 1L);
    }
  }

  /** Minimal application that triggers the initial resolver load the way the service does. */
  @Configuration
  static class FailingTrustAnchorConfiguration {

    @Bean
    ResolverStateManager resolverStateManager() {
      startedManager = new ResolverStateManager(new TrustAnchorTreeLoader(), new AlwaysFreeLock(),
          ObservationRegistry.NOOP);
      return startedManager;
    }

    @Bean
    RegistryReadyPublisher registryReadyPublisher() {
      return new RegistryReadyPublisher();
    }
  }

  /** Stands in for the registry state manager, which publishes the event the resolver loads from. */
  static class RegistryReadyPublisher {
    @EventListener
    RegistryReadyEvent init(final ApplicationStartedEvent event) {
      return new RegistryReadyEvent();
    }
  }

  private static class AlwaysFreeLock implements ServiceLock {
    @Override
    public boolean acquireLock(final String name) {
      return true;
    }

    @Override
    public void close(final String name) {
    }
  }
}
