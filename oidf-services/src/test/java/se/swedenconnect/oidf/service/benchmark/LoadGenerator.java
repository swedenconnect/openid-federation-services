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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Closed-loop HTTP load generator: a fixed number of workers, each sending its next request when the previous
 * one completed.
 *
 * @author Felix Hellman
 */
public class LoadGenerator {

  private final HttpClient client = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .connectTimeout(Duration.ofSeconds(5))
      .executor(Executors.newVirtualThreadPerTaskExecutor())
      .build();

  /**
   * A kind of request.
   *
   * @param name           name in the report
   * @param uri            supplies the uri of the next request
   * @param expectedStatus status that counts as success
   */
  public record Scenario(String name, Supplier<URI> uri, int expectedStatus) {
  }

  /**
   * One completed request.
   *
   * @param scenario     scenario name
   * @param startNanos   System.nanoTime() when the request was sent
   * @param latencyNanos time until the full response was read
   * @param status       http status, or -1 on transport error/timeout
   * @param expected     true if status was the expected one
   */
  public record Sample(String scenario, long startNanos, long latencyNanos, int status, boolean expected) {
  }

  /**
   * Summary of a set of samples.
   *
   * @param name       scenario name
   * @param cache      "cached" or "no-cache"
   * @param requests   number of requests
   * @param rps        requests per second
   * @param p50Ms      median latency
   * @param p95Ms      95th percentile latency
   * @param p99Ms      99th percentile latency
   * @param maxMs      max latency
   * @param unexpected requests that did not get the expected status
   * @param statuses   count per status
   */
  public record Summary(String name, String cache, int requests, double rps, double p50Ms, double p95Ms,
                        double p99Ms, double maxMs, long unexpected, Map<Integer, Long> statuses) {
  }

  /**
   * Runs a fixed number of requests of one scenario.
   *
   * @param scenario    to run
   * @param requests    number of requests
   * @param concurrency number of concurrent workers
   * @param noCache     true to send cache-control: no-cache
   * @return summary
   * @throws InterruptedException if interrupted
   */
  public Summary run(final Scenario scenario, final int requests, final int concurrency, final boolean noCache)
      throws InterruptedException {
    final ConcurrentLinkedQueue<Sample> samples = new ConcurrentLinkedQueue<>();
    final AtomicInteger remaining = new AtomicInteger(requests);
    final long start = System.nanoTime();
    try (final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int w = 0; w < concurrency; w++) {
        workers.submit(() -> {
          while (remaining.getAndDecrement() > 0) {
            samples.add(this.send(scenario, noCache));
          }
        });
      }
    }
    final double seconds = (System.nanoTime() - start) / 1e9;
    return summarize(scenario.name(), noCache, new ArrayList<>(samples), seconds);
  }

  /**
   * Runs scenarios round-robin in the background until stopped.
   *
   * @param scenarios   to run
   * @param concurrency number of concurrent workers
   * @param noCache     true to send cache-control: no-cache
   * @return handle to stop the load and collect the samples
   */
  public Background background(final List<Scenario> scenarios, final int concurrency, final boolean noCache) {
    return new Background(scenarios, concurrency, noCache);
  }

  /**
   * Load running in the background.
   */
  public final class Background {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ConcurrentLinkedQueue<Sample> samples = new ConcurrentLinkedQueue<>();
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

    private Background(final List<Scenario> scenarios, final int concurrency, final boolean noCache) {
      final AtomicInteger next = new AtomicInteger();
      for (int w = 0; w < concurrency; w++) {
        this.workers.submit(() -> {
          while (this.running.get()) {
            final Scenario scenario = scenarios.get(Math.floorMod(next.getAndIncrement(), scenarios.size()));
            this.samples.add(LoadGenerator.this.send(scenario, noCache));
          }
        });
      }
    }

    /**
     * @return samples collected so far
     */
    public List<Sample> samples() {
      return new ArrayList<>(this.samples);
    }

    /**
     * Stops the load and waits for in-flight requests.
     *
     * @return all samples
     * @throws InterruptedException if interrupted
     */
    public List<Sample> stop() throws InterruptedException {
      this.running.set(false);
      this.workers.shutdown();
      this.workers.awaitTermination(1, TimeUnit.MINUTES);
      return this.samples();
    }
  }

  /**
   * Sends a single GET.
   *
   * @param uri     to get
   * @param noCache true to send cache-control: no-cache
   * @return response, or null on transport error
   * @throws InterruptedException if interrupted
   */
  public HttpResponse<String> get(final URI uri, final boolean noCache) throws InterruptedException {
    try {
      return this.client.send(this.request(uri, noCache).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    catch (final java.io.IOException e) {
      return null;
    }
  }

  /**
   * Sends a single POST.
   *
   * @param uri  to post to
   * @param body request body
   * @return response, or null on transport error
   * @throws InterruptedException if interrupted
   */
  public HttpResponse<String> post(final URI uri, final String body) throws InterruptedException {
    try {
      return this.client.send(this.request(uri, false).POST(HttpRequest.BodyPublishers.ofString(body)).build(),
          HttpResponse.BodyHandlers.ofString());
    }
    catch (final java.io.IOException e) {
      return null;
    }
  }

  /**
   * Summarizes samples.
   *
   * @param name    name in the report
   * @param noCache true if no-cache was sent
   * @param samples to summarize
   * @param seconds wall time the samples were collected over
   * @return summary
   */
  public static Summary summarize(final String name, final boolean noCache, final List<Sample> samples,
      final double seconds) {
    final long[] latencies = samples.stream().mapToLong(Sample::latencyNanos).sorted().toArray();
    final Map<Integer, Long> statuses = new TreeMap<>();
    samples.forEach(s -> statuses.merge(s.status(), 1L, Long::sum));
    return new Summary(
        name,
        noCache ? "no-cache" : "cached",
        samples.size(),
        seconds > 0 ? samples.size() / seconds : 0,
        percentile(latencies, 0.50),
        percentile(latencies, 0.95),
        percentile(latencies, 0.99),
        latencies.length == 0 ? 0 : latencies[latencies.length - 1] / 1e6,
        samples.stream().filter(s -> !s.expected()).count(),
        statuses);
  }

  private static double percentile(final long[] sorted, final double p) {
    if (sorted.length == 0) {
      return 0;
    }
    final int index = (int) Math.min(sorted.length - 1, Math.ceil(p * sorted.length) - 1);
    return sorted[Math.max(0, index)] / 1e6;
  }

  private Sample send(final Scenario scenario, final boolean noCache) {
    final long start = System.nanoTime();
    int status;
    try {
      status = this.client.send(this.request(scenario.uri().get(), noCache).GET().build(),
          HttpResponse.BodyHandlers.discarding()).statusCode();
    }
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      status = -1;
    }
    catch (final Exception e) {
      status = -1;
    }
    return new Sample(scenario.name(), start, System.nanoTime() - start, status, status == scenario.expectedStatus());
  }

  private HttpRequest.Builder request(final URI uri, final boolean noCache) {
    final HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30));
    if (noCache) {
      builder.header("cache-control", "no-cache");
    }
    return builder;
  }

  static List<Sample> window(final List<Sample> samples, final long fromNanos, final long toNanos) {
    return samples.stream().filter(s -> s.startNanos() >= fromNanos && s.startNanos() < toNanos).toList();
  }
}
