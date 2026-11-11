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
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;

import java.util.Map;

/**
 * Renders a benchmark report as markdown.
 *
 * @author Felix Hellman
 */
public final class BenchmarkReport {

  private static final double MB = 1024.0 * 1024.0;

  private BenchmarkReport() {
  }

  /**
   * @param report the report as written to json
   * @return markdown summary
   */
  public static String markdown(final Map<String, Object> report) {
    final JsonObject root = new Gson().toJsonTree(report).getAsJsonObject();
    final StringBuilder md = new StringBuilder();
    md.append("# Registry benchmark: ").append(root.get("label").getAsString()).append("\n\n");
    md.append("Options: `").append(root.get("options")).append("`\n\n");

    md.append("## Summary\n\n");
    md.append("p99 latencies are no-cache. Reload times are max over all reloads, from notify until visible.\n\n");
    md.append("| leaves | wire MB | ready s | EC p99 ms | fetch p99 ms | tm-status p99 ms | unmatched p99 ms "
        + "| reload→registry ms | reload→resolver ms | reload→cached ms | failed during reload "
        + "| retained heap MB | redis MB |\n");
    md.append("|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
    for (final JsonElement element : root.getAsJsonArray("runs")) {
      final JsonObject run = element.getAsJsonObject();
      if (run.has("error") || !run.has("reloads")) {
        md.append("| ").append(run.get("leaves")).append(" | failed: ")
            .append(run.has("error") ? run.get("error").getAsString() : "incomplete").append(" |\n");
        continue;
      }
      final JsonObject payload = run.getAsJsonObject("payload");
      final JsonObject resources = run.getAsJsonObject("resources");
      md.append("| ").append(run.get("leaves").getAsInt())
          .append(" | ").append(fmt((payload.get("entityWireBytes").getAsLong()
              + payload.get("moduleWireBytes").getAsLong()) / MB))
          .append(" | ").append(fmt(run.getAsJsonObject("startup").get("readyMillis").getAsLong() / 1000.0))
          .append(" | ").append(p99(run, "entity-configuration"))
          .append(" | ").append(p99(run, "fetch"))
          .append(" | ").append(p99(run, "trust-mark-status"))
          .append(" | ").append(p99(run, "unmatched"))
          .append(" | ").append(maxReload(run, "registryConvergedMillis"))
          .append(" | ").append(maxReload(run, "resolverConvergedMillis"))
          .append(" | ").append(maxReload(run, "cachedConvergedMillis"))
          .append(" | ").append(failedDuringReload(run))
          .append(" | ").append(fmt(resources.get("retainedHeapBytes").getAsDouble() / MB))
          .append(" | ").append(fmt(resources.get("redisUsedMemoryBytes").getAsDouble() / MB))
          .append(" |\n");
    }

    for (final JsonElement element : root.getAsJsonArray("runs")) {
      final JsonObject run = element.getAsJsonObject();
      if (run.has("error") || !run.has("reloads")) {
        continue;
      }
      md.append("\n## ").append(run.get("leaves").getAsInt()).append(" leaves (")
          .append(run.get("intermediates").getAsInt()).append(" intermediates)\n\n");

      if (!run.getAsJsonArray("smokeFailures").isEmpty()) {
        md.append("**Smoke failures** (scenario results below are not meaningful for these):\n\n");
        run.getAsJsonArray("smokeFailures").forEach(f -> md.append("- `").append(f.getAsString()).append("`\n"));
        md.append('\n');
      }

      final JsonObject payload = run.getAsJsonObject("payload");
      md.append("Payload: entity_records ").append(fmt(payload.get("entityJsonBytes").getAsLong() / 1024.0))
          .append(" KB json / ").append(fmt(payload.get("entityWireBytes").getAsLong() / 1024.0))
          .append(" KB wire, module_records ").append(fmt(payload.get("moduleJsonBytes").getAsLong() / 1024.0))
          .append(" KB json / ").append(fmt(payload.get("moduleWireBytes").getAsLong() / 1024.0))
          .append(" KB wire.\n\n");

      md.append("| scenario | cache | rps | p50 ms | p95 ms | p99 ms | max ms | unexpected | statuses |\n");
      md.append("|---|---|---:|---:|---:|---:|---:|---:|---|\n");
      for (final JsonElement s : run.getAsJsonArray("scenarios")) {
        final JsonObject summary = s.getAsJsonObject();
        md.append(row(summary, summary.get("name").getAsString()));
      }

      final JsonObject reloads = run.getAsJsonObject("reloads");
      md.append("\nBackground load (no-cache) during reloads:\n\n");
      md.append("| reload | →registry ms | →resolver ms | →cached ms | peak heap MB | rps | p99 ms | max ms "
          + "| unexpected | by scenario |\n");
      md.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---|\n");
      final JsonObject baseline = reloads.getAsJsonObject("baseline");
      md.append("| baseline | | | | | ").append(fmt(baseline.get("rps").getAsDouble()))
          .append(" | ").append(fmt(baseline.get("p99Ms").getAsDouble()))
          .append(" | ").append(fmt(baseline.get("maxMs").getAsDouble()))
          .append(" | ").append(baseline.get("unexpected").getAsLong()).append(" | |\n");
      int i = 1;
      for (final JsonElement r : reloads.getAsJsonArray("reloads")) {
        final JsonObject reload = r.getAsJsonObject();
        final JsonObject during = reload.getAsJsonObject("duringReload");
        md.append("| ").append(i++)
            .append(" | ").append(reload.get("registryConvergedMillis").getAsLong())
            .append(" | ").append(reload.get("resolverConvergedMillis").getAsLong())
            .append(" | ").append(reload.get("cachedConvergedMillis").getAsLong())
            .append(" | ").append(fmt(reload.get("peakHeapBytes").getAsDouble() / MB))
            .append(" | ").append(fmt(during.get("rps").getAsDouble()))
            .append(" | ").append(fmt(during.get("p99Ms").getAsDouble()))
            .append(" | ").append(fmt(during.get("maxMs").getAsDouble()))
            .append(" | ").append(during.get("unexpected").getAsLong())
            .append(" | `").append(reload.get("unexpectedByScenario")).append("` |\n");
      }

      final JsonObject resources = run.getAsJsonObject("resources");
      md.append("\nResources: retained heap after ready ")
          .append(fmt(run.getAsJsonObject("startup").get("retainedHeapAfterReadyBytes").getAsDouble() / MB))
          .append(" MB, after scenarios ").append(fmt(resources.get("retainedHeapBytes").getAsDouble() / MB))
          .append(" MB (full GC forced), GC pauses ").append(resources.get("gcPauseCount").getAsLong())
          .append(" (total ").append(fmt(resources.get("gcPauseTotalMillis").getAsDouble()))
          .append(" ms, max ").append(fmt(resources.get("gcPauseMaxMillis").getAsDouble()))
          .append(" ms), redis ").append(fmt(resources.get("redisUsedMemoryBytes").getAsDouble() / MB))
          .append(" MB in ").append(resources.get("redisKeys").getAsLong())
          .append(" keys, registry served ").append(fmt(resources.get("registryBytesServed").getAsDouble() / MB))
          .append(" MB.\n");
    }
    return md.toString();
  }

  private static String row(final JsonObject summary, final String name) {
    return "| %s | %s | %s | %s | %s | %s | %s | %d | `%s` |\n".formatted(
        name,
        summary.get("cache").getAsString(),
        fmt(summary.get("rps").getAsDouble()),
        fmt(summary.get("p50Ms").getAsDouble()),
        fmt(summary.get("p95Ms").getAsDouble()),
        fmt(summary.get("p99Ms").getAsDouble()),
        fmt(summary.get("maxMs").getAsDouble()),
        summary.get("unexpected").getAsLong(),
        summary.get("statuses"));
  }

  private static String p99(final JsonObject run, final String scenario) {
    for (final JsonElement s : run.getAsJsonArray("scenarios")) {
      final JsonObject summary = s.getAsJsonObject();
      if (scenario.equals(summary.get("name").getAsString()) && "no-cache".equals(summary.get("cache").getAsString())) {
        return fmt(summary.get("p99Ms").getAsDouble());
      }
    }
    return "-";
  }

  private static String maxReload(final JsonObject run, final String field) {
    long max = Long.MIN_VALUE;
    boolean timedOut = false;
    for (final JsonElement r : run.getAsJsonObject("reloads").getAsJsonArray("reloads")) {
      final long value = r.getAsJsonObject().get(field).getAsLong();
      timedOut |= value < 0;
      max = Math.max(max, value);
    }
    if (max == Long.MIN_VALUE) {
      return "-";
    }
    return timedOut ? "timeout" : String.valueOf(max);
  }

  private static long failedDuringReload(final JsonObject run) {
    long failed = 0;
    for (final JsonElement r : run.getAsJsonObject("reloads").getAsJsonArray("reloads")) {
      failed += r.getAsJsonObject().getAsJsonObject("duringReload").get("unexpected").getAsLong();
    }
    return failed;
  }

  private static String fmt(final double value) {
    return value >= 100 ? "%.0f".formatted(value) : "%.2f".formatted(value);
  }
}
