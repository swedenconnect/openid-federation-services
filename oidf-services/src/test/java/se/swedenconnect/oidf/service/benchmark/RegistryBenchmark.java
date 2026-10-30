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
package se.swedenconnect.oidf.service.benchmark;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jwt.SignedJWT;
import com.redis.testcontainers.RedisContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * End-to-end benchmark of registry integration: a fake registry serving a synthetic federation of a given size,
 * the service in its own JVM, and real Redis.
 *
 * <p>Everything is measured from the outside (federation endpoints, actuator, redis INFO), so the benchmark stays
 * valid regardless of how the service is changed internally. Run through {@code run-registry-benchmark.sh}.</p>
 *
 * <p>Per federation size it measures:</p>
 * <ol>
 *   <li>registry payload size</li>
 *   <li>time from start until /actuator/ready</li>
 *   <li>request latency per endpoint, with and without cache</li>
 *   <li>reloads under load: time from notify until a change is visible, and failed requests during the reload</li>
 *   <li>heap, live data after GC, GC pauses and redis memory</li>
 * </ol>
 *
 * @author Felix Hellman
 */
public final class RegistryBenchmark {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Pattern USED_MEMORY = Pattern.compile("used_memory:(\\d+)");

  private final Options options;
  private final String name;
  private final LoadGenerator load = new LoadGenerator();

  private RegistryBenchmark(final Options options, final String name) {
    this.options = options;
    this.name = name;
  }

  /**
   * Benchmark options, read from system properties.
   *
   * @param sizes                 leaf counts to run, one fresh service + redis per size
   * @param leavesPerIntermediate leaves per intermediate
   * @param trustMarkTypes        trust mark types, every leaf is a subject of each
   * @param extraMetadataBytes    padding per leaf metadata
   * @param resolver              configure a resolver for the trust anchor
   * @param virtualEntityRouting  value of federation.routing.virtual-entity-routing.enabled
   * @param heap                  -Xmx of the service
   * @param requests              requests per scenario
   * @param concurrency           concurrent requests
   * @param reloads               number of reloads under load
   * @param readyTimeout          max time to wait for the service to become ready
   * @param convergenceTimeout    max time to wait for a change to become visible
   * @param outputDir             where reports and service logs are written
   * @param label                 label of this run, e.g. a git commit
   * @param redisImage            redis docker image
   */
  public record Options(List<Integer> sizes, int leavesPerIntermediate, int trustMarkTypes, int extraMetadataBytes,
                        boolean resolver, boolean virtualEntityRouting, String heap, int requests, int concurrency,
                        int reloads, Duration readyTimeout, Duration convergenceTimeout, Path outputDir,
                        String label, String redisImage) {

    static Options fromSystemProperties() {
      return new Options(
          Arrays.stream(System.getProperty("benchmark.sizes", "100,1000,5000").split(","))
              .map(String::trim).map(Integer::parseInt).toList(),
          Integer.getInteger("benchmark.leavesPerIntermediate", 500),
          Integer.getInteger("benchmark.trustMarkTypes", 2),
          Integer.getInteger("benchmark.extraMetadataBytes", 0),
          Boolean.parseBoolean(System.getProperty("benchmark.resolver", "true")),
          Boolean.parseBoolean(System.getProperty("benchmark.virtualEntityRouting", "true")),
          System.getProperty("benchmark.heap", "1g"),
          Integer.getInteger("benchmark.requests", 2000),
          Integer.getInteger("benchmark.concurrency", 16),
          Integer.getInteger("benchmark.reloads", 3),
          Duration.parse(System.getProperty("benchmark.readyTimeout", "PT15M")),
          Duration.parse(System.getProperty("benchmark.convergenceTimeout", "PT2M")),
          Path.of(System.getProperty("benchmark.outputDir", "oidf-services/target/benchmark")),
          System.getProperty("benchmark.label", "unlabeled"),
          System.getProperty("benchmark.redisImage", "redis/redis-stack:latest"));
    }

    Map<String, Object> toMap() {
      final Map<String, Object> map = new LinkedHashMap<>();
      map.put("sizes", this.sizes);
      map.put("leavesPerIntermediate", this.leavesPerIntermediate);
      map.put("trustMarkTypes", this.trustMarkTypes);
      map.put("extraMetadataBytes", this.extraMetadataBytes);
      map.put("resolver", this.resolver);
      map.put("virtualEntityRouting", this.virtualEntityRouting);
      map.put("heap", this.heap);
      map.put("requests", this.requests);
      map.put("concurrency", this.concurrency);
      map.put("reloads", this.reloads);
      return map;
    }
  }

  /**
   * Runs the benchmark.
   *
   * @param args not used, configure with -Dbenchmark.* system properties
   * @throws Exception on failure
   */
  public static void main(final String[] args) throws Exception {
    Locale.setDefault(Locale.ROOT);
    final Options options = Options.fromSystemProperties();
    final String name = "registry-benchmark-%s-%s".formatted(
        options.label().replaceAll("[^A-Za-z0-9._-]", "_"),
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
    final RegistryBenchmark benchmark = new RegistryBenchmark(options, name);
    final List<Map<String, Object>> runs = new ArrayList<>();
    for (final int size : options.sizes()) {
      log("=== %d leaves ===".formatted(size));
      runs.add(benchmark.runSize(size));
    }

    final Map<String, Object> report = new LinkedHashMap<>();
    report.put("label", options.label());
    report.put("timestamp", LocalDateTime.now().toString());
    report.put("options", options.toMap());
    report.put("runs", runs);

    Files.createDirectories(options.outputDir());
    final Path json = options.outputDir().resolve(name + ".json");
    final Path markdown = options.outputDir().resolve(name + ".md");
    Files.writeString(json, GSON.toJson(report));
    final String summary = BenchmarkReport.markdown(report);
    Files.writeString(markdown, summary);
    System.out.println();
    System.out.println(summary);
    log("Wrote %s and %s".formatted(json, markdown));
    System.exit(0);
  }

  private Map<String, Object> runSize(final int leaves) throws Exception {
    final Map<String, Object> run = new LinkedHashMap<>();
    run.put("leaves", leaves);
    try (final RedisContainer redis = new RedisContainer(DockerImageName.parse(this.options.redisImage()));
         final BenchmarkRegistry registry = new BenchmarkRegistry()) {
      redis.start();
      final int port = ServiceProcess.freePort();
      final SyntheticFederation federation = new SyntheticFederation(
          "http://localhost:%d/bench".formatted(port),
          leaves,
          this.options.leavesPerIntermediate(),
          this.options.trustMarkTypes(),
          this.options.extraMetadataBytes(),
          this.options.resolver(),
          registry.publicJwk(),
          registry.hostedKeyReference());
      final BenchmarkRegistry.Payload payload = registry.publish(federation);
      run.put("intermediates", federation.intermediates());
      run.put("payload", payload);
      log("Payload: entity %.1f KB, module %.1f KB on the wire".formatted(
          payload.entityWireBytes() / 1024.0, payload.moduleWireBytes() / 1024.0));

      final Map<String, String> properties = new LinkedHashMap<>();
      properties.put("spring.data.redis.url", redis.getRedisURI());
      properties.put("federation.registry.integration.client.base-uri", registry.baseUri());
      properties.put("federation.local-registry.entities", "");
      properties.put("federation.local-registry.resolvers", "");
      properties.put("federation.local-registry.trust-anchors", "");
      properties.put("federation.local-registry.trust-mark-issuers", "");
      properties.put("federation.routing.virtual-entity-routing.enabled",
          String.valueOf(this.options.virtualEntityRouting()));
      properties.put("logging.level.se.swedenconnect", "WARN");

      final Path serviceLog = this.options.outputDir().resolve("%s-service-%d.log".formatted(this.name, leaves));
      try (final ServiceProcess service = ServiceProcess.start(port, this.options.heap(), properties, serviceLog)) {
        run.put("serviceLog", serviceLog.toString());
        final Duration ready = service.awaitReady(this.options.readyTimeout());
        final Map<String, Object> startup = new LinkedHashMap<>();
        startup.put("readyMillis", ready.toMillis());
        startup.put("registryEntityFetches", registry.entityRequests());
        startup.put("registryModuleFetches", registry.moduleRequests());
        startup.put("retainedHeapAfterReadyBytes", service.retainedHeap());
        run.put("startup", startup);
        log("Ready after %d ms".formatted(ready.toMillis()));

        final List<LoadGenerator.Scenario> scenarios = this.scenarios(federation, service);
        run.put("smokeFailures", this.smoke(scenarios));

        final List<LoadGenerator.Summary> summaries = new ArrayList<>();
        for (final LoadGenerator.Scenario scenario : scenarios) {
          this.load.run(scenario, Math.max(50, this.options.requests() / 4), this.options.concurrency(), true);
          for (final boolean noCache : List.of(false, true)) {
            final LoadGenerator.Summary summary =
                this.load.run(scenario, this.options.requests(), this.options.concurrency(), noCache);
            log("%-22s %-8s %8.0f rps  p50 %7.2f  p99 %7.2f ms  unexpected %d".formatted(summary.name(),
                summary.cache(), summary.rps(), summary.p50Ms(), summary.p99Ms(), summary.unexpected()));
            summaries.add(summary);
            run.put("scenarios", summaries);
            service.assertAlive("scenario " + summary.name() + " " + summary.cache());
          }
        }
        run.put("scenarios", summaries);

        final Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("retainedHeapBytes", service.retainedHeap());
        resources.put("redisUsedMemoryBytes", redisUsedMemory(redis));
        resources.put("redisKeys", redisKeys(redis));
        run.put("resources", resources);

        run.put("reloads", this.reloads(federation, registry, service, scenarios));

        resources.put("gcPauseCount", service.metric("jvm.gc.pause", "COUNT").orElse(-1));
        resources.put("gcPauseTotalMillis", service.metricMillis("jvm.gc.pause", "TOTAL_TIME").orElse(-1));
        resources.put("gcPauseMaxMillis", service.metricMillis("jvm.gc.pause", "MAX").orElse(-1));
        resources.put("registryBytesServed", registry.bytesServed());
        resources.put("serviceAlive", service.alive());
      }
    }
    catch (final Exception e) {
      log("Run with %d leaves failed: %s".formatted(leaves, e.getMessage()));
      run.put("error", String.valueOf(e.getMessage()));
    }
    return run;
  }

  private List<LoadGenerator.Scenario> scenarios(final SyntheticFederation federation, final ServiceProcess service)
      throws Exception {
    final List<LoadGenerator.Scenario> scenarios = new ArrayList<>();
    scenarios.add(new LoadGenerator.Scenario("entity-configuration",
        () -> URI.create(federation.leaf(randomLeaf(federation)) + "/.well-known/openid-federation"), 200));
    scenarios.add(new LoadGenerator.Scenario("fetch", () -> {
      final int leaf = randomLeaf(federation);
      return URI.create(federation.intermediateOf(leaf) + "/fetch?sub=" + encode(federation.leaf(leaf)));
    }, 200));
    scenarios.add(new LoadGenerator.Scenario("subordinate-listing", () -> URI.create(
        federation.intermediate(ThreadLocalRandom.current().nextInt(federation.intermediates()))
            + "/subordinate_listing"), 200));
    scenarios.add(new LoadGenerator.Scenario("trust-mark", () -> URI.create(
        federation.trustMarkIssuer() + "/trust_mark?trust_mark_type="
            + encode(federation.trustMarkType(ThreadLocalRandom.current().nextInt(federation.trustMarkTypes())))
            + "&sub=" + encode(federation.leaf(randomLeaf(federation)))), 200));

    final List<String> trustMarks = new ArrayList<>();
    for (int i = 0; i < 32; i++) {
      final HttpResponse<String> response = this.load.get(URI.create(federation.trustMarkIssuer()
          + "/trust_mark?trust_mark_type=" + encode(federation.trustMarkType(0))
          + "&sub=" + encode(federation.leaf(randomLeaf(federation)))), false);
      if (response != null && response.statusCode() == 200) {
        trustMarks.add(response.body());
      }
    }
    if (!trustMarks.isEmpty()) {
      scenarios.add(new LoadGenerator.Scenario("trust-mark-status", () -> URI.create(
          federation.trustMarkIssuer() + "/trust_mark_status?trust_mark="
              + encode(trustMarks.get(ThreadLocalRandom.current().nextInt(trustMarks.size())))), 200));
    }
    else {
      log("No trust marks could be issued, skipping trust-mark-status");
    }
    scenarios.add(new LoadGenerator.Scenario("trust-mark-listing", () -> URI.create(
        federation.trustMarkIssuer() + "/trust_mark_listing?trust_mark_type=" + encode(federation.trustMarkType(0))
            + "&sub=" + encode(federation.leaf(randomLeaf(federation)))), 200));
    if (federation.hasResolver()) {
      scenarios.add(new LoadGenerator.Scenario("resolve", () -> URI.create(
          federation.resolverEntity() + "/resolve?sub=" + encode(federation.leaf(randomLeaf(federation)))
              + "&trust_anchor=" + encode(federation.trustAnchor())), 200));
    }
    scenarios.add(new LoadGenerator.Scenario("unmatched", () -> service.uri(
        "/bench/unknown-%d/fetch?sub=x".formatted(ThreadLocalRandom.current().nextInt(1_000_000))), 404));
    return scenarios;
  }

  /**
   * Sends one request per scenario, so that a misconfigured scenario shows up as such instead of as a fast error.
   */
  private List<String> smoke(final List<LoadGenerator.Scenario> scenarios) throws InterruptedException {
    final List<String> failures = new ArrayList<>();
    for (final LoadGenerator.Scenario scenario : scenarios) {
      final URI uri = scenario.uri().get();
      final HttpResponse<String> response = this.load.get(uri, true);
      final int status = response == null ? -1 : response.statusCode();
      if (status != scenario.expectedStatus()) {
        final String body = response == null ? "" : response.body();
        final String failure = "%s: expected %d got %d for %s %s".formatted(scenario.name(),
            scenario.expectedStatus(), status, uri, body.substring(0, Math.min(200, body.length())));
        log("SMOKE FAILURE " + failure);
        failures.add(failure);
      }
    }
    return failures;
  }

  private Map<String, Object> reloads(
      final SyntheticFederation federation,
      final BenchmarkRegistry registry,
      final ServiceProcess service,
      final List<LoadGenerator.Scenario> scenarios) throws Exception {

    final List<LoadGenerator.Scenario> background = scenarios.stream()
        .filter(s -> List.of("entity-configuration", "fetch", "trust-mark", "resolve").contains(s.name()))
        .toList();
    final long baselineStart = System.nanoTime();
    final LoadGenerator.Background load =
        this.load.background(background, Math.max(1, this.options.concurrency() / 2), true);
    Thread.sleep(3000);
    final long baselineEnd = System.nanoTime();

    final List<Map<String, Object>> reloads = new ArrayList<>();
    final List<long[]> windows = new ArrayList<>();
    for (int r = 0; r < this.options.reloads(); r++) {
      federation.bumpRevision();
      final BenchmarkRegistry.Payload payload = registry.publish(federation);
      final String expected = federation.markerOrganizationName();
      final URI ec = URI.create(federation.markerLeaf() + "/.well-known/openid-federation");
      final URI resolve = URI.create(federation.resolverEntity() + "/resolve?sub=" + encode(federation.markerLeaf())
          + "&trust_anchor=" + encode(federation.trustAnchor()));

      final AtomicLong peakHeap = new AtomicLong();
      final AtomicBoolean sampling = new AtomicBoolean(true);
      final Thread sampler = Thread.ofVirtual().start(() -> {
        while (sampling.get()) {
          peakHeap.accumulateAndGet((long) service.heapUsed(), Math::max);
          try {
            Thread.sleep(200);
          }
          catch (final InterruptedException e) {
            return;
          }
        }
      });

      final long start = System.nanoTime();
      final HttpResponse<String> notify = this.load.post(service.uri("/registry/notify"), registry.notification());
      final long registryMillis = this.awaitMillis(start, () -> expected.equals(this.organizationName(ec, true)));
      final long resolverMillis = federation.hasResolver()
          ? this.awaitMillis(start, () -> expected.equals(this.organizationName(resolve, true)))
          : -1;
      final long cachedMillis = this.awaitMillis(start, () -> expected.equals(this.organizationName(ec, false)));
      Thread.sleep(1000);
      final long end = System.nanoTime();
      sampling.set(false);
      sampler.join();
      windows.add(new long[] {start, end});

      final Map<String, Object> reload = new LinkedHashMap<>();
      reload.put("notifyStatus", notify == null ? -1 : notify.statusCode());
      reload.put("payloadWireBytes", payload.entityWireBytes() + payload.moduleWireBytes());
      reload.put("registryConvergedMillis", registryMillis);
      reload.put("resolverConvergedMillis", resolverMillis);
      reload.put("cachedConvergedMillis", cachedMillis);
      reload.put("peakHeapBytes", peakHeap.get());
      reloads.add(reload);
      log("Reload %d: registry %d ms, resolver %d ms, cached %d ms".formatted(
          r + 1, registryMillis, resolverMillis, cachedMillis));
      if (!service.alive()) {
        load.stop();
      }
      service.assertAlive("reload " + (r + 1));
      Thread.sleep(2000);
    }

    final List<LoadGenerator.Sample> samples = load.stop();
    final Map<String, Object> result = new LinkedHashMap<>();
    result.put("baseline", LoadGenerator.summarize("background", true,
        LoadGenerator.window(samples, baselineStart, baselineEnd), (baselineEnd - baselineStart) / 1e9));
    for (int r = 0; r < reloads.size(); r++) {
      final long[] window = windows.get(r);
      final List<LoadGenerator.Sample> during = LoadGenerator.window(samples, window[0], window[1]);
      reloads.get(r).put("duringReload", LoadGenerator.summarize("background", true, during,
          (window[1] - window[0]) / 1e9));
      reloads.get(r).put("unexpectedByScenario", unexpectedByScenario(during));
    }
    result.put("reloads", reloads);
    return result;
  }

  private String organizationName(final URI uri, final boolean noCache) {
    try {
      final HttpResponse<String> response = this.load.get(uri, noCache);
      if (response == null || response.statusCode() != 200) {
        return null;
      }
      final Map<String, Object> metadata = SignedJWT.parse(response.body()).getJWTClaimsSet()
          .getJSONObjectClaim("metadata");
      if (metadata == null || !(metadata.get("federation_entity") instanceof Map<?, ?> federationEntity)) {
        return null;
      }
      return String.valueOf(federationEntity.get("organization_name"));
    }
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
    catch (final Exception e) {
      return null;
    }
  }

  private long awaitMillis(final long startNanos, final BooleanSupplier condition) throws InterruptedException {
    final long deadline = startNanos + this.options.convergenceTimeout().toNanos();
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) {
        return (System.nanoTime() - startNanos) / 1_000_000;
      }
      Thread.sleep(25);
    }
    return -1;
  }

  private static Map<String, Map<Integer, Long>> unexpectedByScenario(final List<LoadGenerator.Sample> samples) {
    final Map<String, Map<Integer, Long>> result = new LinkedHashMap<>();
    samples.stream().filter(s -> !s.expected()).forEach(s ->
        result.computeIfAbsent(s.scenario(), k -> new LinkedHashMap<>()).merge(s.status(), 1L, Long::sum));
    return result;
  }

  private static long redisUsedMemory(final RedisContainer redis) throws Exception {
    final Matcher matcher = USED_MEMORY.matcher(redis.execInContainer("redis-cli", "INFO", "memory").getStdout());
    return matcher.find() ? Long.parseLong(matcher.group(1)) : -1;
  }

  private static long redisKeys(final RedisContainer redis) throws Exception {
    return Long.parseLong(redis.execInContainer("redis-cli", "DBSIZE").getStdout().trim());
  }

  private static int randomLeaf(final SyntheticFederation federation) {
    return ThreadLocalRandom.current().nextInt(federation.leaves());
  }

  private static String encode(final String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static void log(final String message) {
    System.out.printf("[benchmark %s] %s%n", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME), message);
  }
}
