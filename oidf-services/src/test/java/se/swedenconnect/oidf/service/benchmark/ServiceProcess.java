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

import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The service under test, running in its own JVM so that heap and GC numbers belong to the service only.
 *
 * <p>Started from the same classpath as the benchmark with the {@code integration-test} profile, which disables
 * the scheduled registry and resolver triggers. Reloads are only triggered through /registry/notify.</p>
 *
 * @author Felix Hellman
 */
public class ServiceProcess implements AutoCloseable {

  private static final String MAIN_CLASS = "se.swedenconnect.oidf.service.Application";

  private final Process process;
  private final int port;
  private final int managementPort;
  private final Path log;
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  private ServiceProcess(final Process process, final int port, final int managementPort, final Path log) {
    this.process = process;
    this.port = port;
    this.managementPort = managementPort;
    this.log = log;
  }

  /**
   * @return a free local port
   * @throws IOException if no port can be allocated
   */
  public static int freePort() throws IOException {
    try (final ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  /**
   * Starts the service.
   *
   * @param port         server port, must match the base url of the synthetic federation
   * @param heap         value for -Xmx, e.g. 1g
   * @param properties   additional spring properties
   * @param log          file to write the service output to
   * @return the started process (not yet ready)
   * @throws IOException if the process can not be started
   */
  public static ServiceProcess start(
      final int port,
      final String heap,
      final Map<String, String> properties,
      final Path log) throws IOException {
    final int managementPort = freePort();
    final List<String> command = new ArrayList<>();
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    command.add("-Xmx" + heap);
    command.add("-XX:+ExitOnOutOfMemoryError");
    command.add("-XX:+HeapDumpOnOutOfMemoryError");
    command.add("-XX:HeapDumpPath=" + log.toString().replaceAll("\\.log$", "") + ".hprof");
    if (Boolean.getBoolean("benchmark.jfr")) {
      final String recording = log.toString().replaceAll("\\.log$", "") + ".jfr";
      command.add("-XX:StartFlightRecording=settings=profile,filename=" + recording + ",dumponexit=true");
    }
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    command.add(MAIN_CLASS);
    command.add("--spring.profiles.active=integration-test");
    command.add("--server.port=" + port);
    command.add("--management.server.port=" + managementPort);
    properties.forEach((key, value) -> command.add("--%s=%s".formatted(key, value)));
    Files.createDirectories(log.getParent());
    final Process process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .redirectOutput(log.toFile())
        .start();
    final ServiceProcess service = new ServiceProcess(process, port, managementPort, log);
    Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));
    return service;
  }

  /**
   * Waits until /actuator/ready reports true.
   *
   * @param timeout max time to wait
   * @return time until ready, measured from now
   * @throws Exception if the service dies or does not become ready in time
   */
  public Duration awaitReady(final Duration timeout) throws Exception {
    final long start = System.nanoTime();
    final long deadline = start + timeout.toNanos();
    while (System.nanoTime() < deadline) {
      if (!this.process.isAlive()) {
        throw new IllegalStateException("Service exited with %d, see %s".formatted(this.process.exitValue(), this.log));
      }
      try {
        final HttpResponse<String> response = this.client.send(
            HttpRequest.newBuilder(this.managementUri("/actuator/ready")).timeout(Duration.ofSeconds(5)).build(),
            HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 && "true".equals(response.body().trim())) {
          return Duration.ofNanos(System.nanoTime() - start);
        }
      }
      catch (final IOException e) {
        // not listening yet
      }
      Thread.sleep(100);
    }
    throw new IllegalStateException("Service not ready within %s, see %s".formatted(timeout, this.log));
  }

  /**
   * Reads a value from /actuator/metrics.
   *
   * @param name      metric name
   * @param statistic statistic to read, e.g. VALUE, COUNT, TOTAL_TIME
   * @param tags      tags in key:value form
   * @return the value, or empty if the metric does not exist
   */
  public OptionalDouble metric(final String name, final String statistic, final String... tags) {
    return this.metric(name, statistic, false, tags);
  }

  /**
   * Reads a time value from /actuator/metrics, converted from the metric's base unit.
   *
   * @param name      metric name
   * @param statistic statistic to read, e.g. TOTAL_TIME, MAX
   * @return the value in milliseconds, or empty if the metric does not exist
   */
  public OptionalDouble metricMillis(final String name, final String statistic) {
    return this.metric(name, statistic, true);
  }

  private OptionalDouble metric(final String name, final String statistic, final boolean millis,
      final String... tags) {
    final StringBuilder path = new StringBuilder("/actuator/metrics/").append(name);
    for (int i = 0; i < tags.length; i++) {
      path.append(i == 0 ? '?' : '&').append("tag=").append(tags[i]);
    }
    try {
      final HttpResponse<String> response = this.client.send(
          HttpRequest.newBuilder(this.managementUri(path.toString())).timeout(Duration.ofSeconds(5)).build(),
          HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return OptionalDouble.empty();
      }
      final JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
      final double factor = millis ? switch (body.has("baseUnit") ? body.get("baseUnit").getAsString() : "") {
        case "seconds" -> 1000.0;
        case "milliseconds" -> 1.0;
        case "nanoseconds" -> 1e-6;
        default -> throw new IllegalStateException("Unknown time unit for " + name + ": " + body.get("baseUnit"));
      } : 1.0;
      for (final JsonElement measurement : body.getAsJsonArray("measurements")) {
        if (statistic.equals(measurement.getAsJsonObject().get("statistic").getAsString())) {
          return OptionalDouble.of(measurement.getAsJsonObject().get("value").getAsDouble() * factor);
        }
      }
      return OptionalDouble.empty();
    }
    catch (final IOException e) {
      return OptionalDouble.empty();
    }
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return OptionalDouble.empty();
    }
  }

  /**
   * @return heap currently in use, in bytes
   */
  public double heapUsed() {
    return this.metric("jvm.memory.used", "VALUE", "area:heap").orElse(-1);
  }

  /**
   * Forces a full GC in the service (jcmd GC.run) and reads the heap in use afterwards, i.e. what the service
   * actually retains.
   *
   * @return retained heap in bytes, or -1 if jcmd failed
   * @throws Exception if interrupted or jcmd can not be started
   */
  public double retainedHeap() throws Exception {
    final Process jcmd = new ProcessBuilder(
        Path.of(System.getProperty("java.home"), "bin", "jcmd").toString(),
        String.valueOf(this.process.pid()),
        "GC.run")
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .start();
    if (!jcmd.waitFor(Duration.ofMinutes(1)) || jcmd.exitValue() != 0) {
      return -1;
    }
    return this.heapUsed();
  }

  /**
   * @param path path on the service port
   * @return uri on the service port
   */
  public URI uri(final String path) {
    return URI.create("http://localhost:%d%s".formatted(this.port, path));
  }

  /**
   * @return true if the service process is still running
   */
  public boolean alive() {
    return this.process.isAlive();
  }

  /**
   * @param phase what was running, for the error message
   * @throws IllegalStateException if the service process has exited
   */
  public void assertAlive(final String phase) {
    if (!this.process.isAlive()) {
      throw new IllegalStateException("Service exited with %d during %s, see %s".formatted(
          this.process.exitValue(), phase, this.log));
    }
  }

  /**
   * @return file with the service output
   */
  public Path log() {
    return this.log;
  }

  @Override
  public void close() throws InterruptedException {
    this.process.destroy();
    if (!this.process.waitFor(Duration.ofSeconds(30))) {
      this.process.destroyForcibly().waitFor();
    }
  }

  private URI managementUri(final String path) {
    return URI.create("http://localhost:%d%s".formatted(this.managementPort, path));
  }
}
