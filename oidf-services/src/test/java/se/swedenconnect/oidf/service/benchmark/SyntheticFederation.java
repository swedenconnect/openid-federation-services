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
import com.nimbusds.jose.shaded.gson.JsonArray;
import com.nimbusds.jose.shaded.gson.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * Generates a synthetic federation as registry records.
 *
 * <pre>
 * ta
 * ├── ia-0 ── leaf-0 .. leaf-(perIa-1)
 * ├── ia-1 ── ...
 * ├── tmi      (one trust mark issuer, every leaf is a subject of every trust mark type)
 * └── resolver (optional, resolves towards ta)
 * </pre>
 *
 * All entities are hosted by the service under test, so the resolver can scrape the whole tree from it.
 *
 * @author Felix Hellman
 */
public class SyntheticFederation {

  private static final Gson GSON = new Gson();

  private final String baseUrl;
  private final int leaves;
  private final int leavesPerIntermediate;
  private final int trustMarkTypes;
  private final int extraMetadataBytes;
  private final boolean resolver;
  private final Map<String, Object> federationJwk;
  private final String hostedKeyReference;
  private int revision;

  /**
   * @param baseUrl               base url of all entities, e.g. http://localhost:11111/bench
   * @param leaves                number of leaf entities
   * @param leavesPerIntermediate leaves per intermediate
   * @param trustMarkTypes        number of trust mark types issued by the trust mark issuer
   * @param extraMetadataBytes    padding added to each leaf's metadata to model richer metadata
   * @param resolver              true if a resolver module should be configured
   * @param federationJwk         public jwk used for subordinate statements and resolver trust
   * @param hostedKeyReference    key reference used for hosted entities, e.g. federation:kid
   */
  public SyntheticFederation(
      final String baseUrl,
      final int leaves,
      final int leavesPerIntermediate,
      final int trustMarkTypes,
      final int extraMetadataBytes,
      final boolean resolver,
      final Map<String, Object> federationJwk,
      final String hostedKeyReference) {
    this.baseUrl = baseUrl;
    this.leaves = leaves;
    this.leavesPerIntermediate = leavesPerIntermediate;
    this.trustMarkTypes = trustMarkTypes;
    this.extraMetadataBytes = extraMetadataBytes;
    this.resolver = resolver;
    this.federationJwk = federationJwk;
    this.hostedKeyReference = hostedKeyReference;
  }

  public String trustAnchor() {
    return this.baseUrl + "/ta";
  }

  public String intermediate(final int k) {
    return this.baseUrl + "/ia-" + k;
  }

  public String leaf(final int i) {
    return this.intermediate(i / this.leavesPerIntermediate) + "/leaf-" + i;
  }

  public String intermediateOf(final int i) {
    return this.intermediate(i / this.leavesPerIntermediate);
  }

  public String trustMarkIssuer() {
    return this.baseUrl + "/tmi";
  }

  public String trustMarkType(final int t) {
    return this.trustMarkIssuer() + "/type-" + t;
  }

  public String resolverEntity() {
    return this.baseUrl + "/resolver";
  }

  public boolean hasResolver() {
    return this.resolver;
  }

  public int leaves() {
    return this.leaves;
  }

  public int intermediates() {
    return Math.max(1, (this.leaves + this.leavesPerIntermediate - 1) / this.leavesPerIntermediate);
  }

  public int trustMarkTypes() {
    return this.trustMarkTypes;
  }

  /**
   * Leaf whose organization name is changed on every {@link #bumpRevision()}.
   *
   * @return entity id of the marker leaf
   */
  public String markerLeaf() {
    return this.leaf(0);
  }

  /**
   * @return organization name the marker leaf has in the current revision
   */
  public String markerOrganizationName() {
    return "bench-rev-" + this.revision;
  }

  /**
   * Changes the marker leaf, so that the next published state differs from the previous one.
   */
  public void bumpRevision() {
    this.revision++;
  }

  /**
   * @return json array for the entity_records claim
   */
  public String entityRecordsJson() {
    final JsonArray records = new JsonArray();
    records.add(this.hostedEntity(this.trustAnchor(), List.of(), this.authorityMetadata(this.trustAnchor())));
    for (int k = 0; k < this.intermediates(); k++) {
      final String ia = this.intermediate(k);
      records.add(this.hostedEntity(ia, List.of(this.trustAnchor()), this.authorityMetadata(ia)));
    }
    final JsonObject tmiMetadata = this.federationEntity("Trust Mark Issuer");
    tmiMetadata.addProperty("federation_trust_mark_endpoint", this.trustMarkIssuer() + "/trust_mark");
    tmiMetadata.addProperty("federation_trust_mark_status_endpoint", this.trustMarkIssuer() + "/trust_mark_status");
    tmiMetadata.addProperty("federation_trust_mark_list_endpoint", this.trustMarkIssuer() + "/trust_mark_listing");
    records.add(this.hostedEntity(this.trustMarkIssuer(), List.of(this.trustAnchor()), tmiMetadata));
    if (this.resolver) {
      final JsonObject resolverMetadata = this.federationEntity("Resolver");
      resolverMetadata.addProperty("federation_resolve_endpoint", this.resolverEntity() + "/resolve");
      records.add(this.hostedEntity(this.resolverEntity(), List.of(this.trustAnchor()), resolverMetadata));
    }
    final String padding = "x".repeat(this.extraMetadataBytes);
    for (int i = 0; i < this.leaves; i++) {
      final JsonObject metadata = this.federationEntity(i == 0 ? this.markerOrganizationName() : "Leaf " + i);
      if (!padding.isEmpty()) {
        metadata.addProperty("bench_padding", padding);
      }
      records.add(this.hostedEntity(this.leaf(i), List.of(this.intermediateOf(i)), metadata));
    }
    return GSON.toJson(records);
  }

  /**
   * @return json object for the module_records claim
   */
  public String moduleRecordsJson() {
    final JsonObject modules = new JsonObject();

    final JsonArray resolvers = new JsonArray();
    if (this.resolver) {
      final JsonObject resolverModule = new JsonObject();
      resolverModule.addProperty("entity-identifier", this.resolverEntity());
      resolverModule.addProperty("trust-anchor", this.trustAnchor());
      resolverModule.add("trusted-keys", this.jwks());
      resolverModule.addProperty("resolve-response-duration", "PT1H");
      resolvers.add(resolverModule);
    }
    modules.add("resolvers", resolvers);

    final JsonArray trustAnchors = new JsonArray();
    final JsonArray taSubordinates = new JsonArray();
    for (int k = 0; k < this.intermediates(); k++) {
      taSubordinates.add(this.subordinate(this.intermediate(k), true));
    }
    taSubordinates.add(this.subordinate(this.trustMarkIssuer(), false));
    if (this.resolver) {
      taSubordinates.add(this.subordinate(this.resolverEntity(), false));
    }
    trustAnchors.add(this.trustAnchorModule(this.trustAnchor(), taSubordinates));
    for (int k = 0; k < this.intermediates(); k++) {
      final JsonArray iaSubordinates = new JsonArray();
      final int from = k * this.leavesPerIntermediate;
      final int to = Math.min(this.leaves, from + this.leavesPerIntermediate);
      for (int i = from; i < to; i++) {
        iaSubordinates.add(this.subordinate(this.leaf(i), false));
      }
      trustAnchors.add(this.trustAnchorModule(this.intermediate(k), iaSubordinates));
    }
    modules.add("trust-anchors", trustAnchors);

    final JsonArray trustMarks = new JsonArray();
    for (int t = 0; t < this.trustMarkTypes; t++) {
      final JsonObject trustMark = new JsonObject();
      trustMark.addProperty("trust-mark-type", this.trustMarkType(t));
      trustMark.addProperty("logo-uri", this.trustMarkType(t) + "/logo");
      trustMark.addProperty("ref-uri", this.trustMarkType(t) + "/ref");
      final JsonArray subjects = new JsonArray();
      for (int i = 0; i < this.leaves; i++) {
        final JsonObject subject = new JsonObject();
        subject.addProperty("sub", this.leaf(i));
        subjects.add(subject);
      }
      trustMark.add("trust-mark-subjects", subjects);
      trustMarks.add(trustMark);
    }
    final JsonObject tmi = new JsonObject();
    tmi.addProperty("entity-identifier", this.trustMarkIssuer());
    tmi.addProperty("trust-mark-validity-duration", "PT30M");
    tmi.add("trust-marks", trustMarks);
    final JsonArray trustMarkIssuers = new JsonArray();
    trustMarkIssuers.add(tmi);
    modules.add("trust-mark-issuers", trustMarkIssuers);

    return GSON.toJson(modules);
  }

  private JsonObject hostedEntity(final String id, final List<String> authorityHints, final JsonObject federationEntity) {
    final JsonObject entity = new JsonObject();
    entity.addProperty("entity-identifier", id);
    entity.addProperty("virtual-entity-id", id);
    entity.addProperty("jwks", this.hostedKeyReference);
    if (!authorityHints.isEmpty()) {
      final JsonArray hints = new JsonArray();
      authorityHints.forEach(hints::add);
      entity.add("authority-hints", hints);
    }
    final JsonObject metadata = new JsonObject();
    metadata.add("federation_entity", federationEntity);
    entity.add("metadata", metadata);
    return entity;
  }

  private JsonObject authorityMetadata(final String id) {
    final JsonObject metadata = this.federationEntity("Authority " + id.substring(this.baseUrl.length()));
    metadata.addProperty("federation_fetch_endpoint", id + "/fetch");
    metadata.addProperty("federation_list_endpoint", id + "/subordinate_listing");
    return metadata;
  }

  private JsonObject federationEntity(final String organizationName) {
    final JsonObject metadata = new JsonObject();
    metadata.addProperty("organization_name", organizationName);
    return metadata;
  }

  private JsonObject trustAnchorModule(final String id, final JsonArray subordinates) {
    final JsonObject module = new JsonObject();
    module.addProperty("entity-identifier", id);
    module.add("subordinates", subordinates);
    return module;
  }

  private JsonObject subordinate(final String id, final boolean intermediate) {
    final JsonObject subordinate = new JsonObject();
    subordinate.addProperty("entity-identifier", id);
    final JsonArray entityTypes = new JsonArray();
    entityTypes.add("federation_entity");
    subordinate.add("entity-types", entityTypes);
    if (intermediate) {
      subordinate.addProperty("is-intermediate", true);
    }
    subordinate.add("jwks", this.jwks());
    return subordinate;
  }

  private JsonObject jwks() {
    final JsonObject jwks = new JsonObject();
    final JsonArray keys = new JsonArray();
    keys.add(GSON.toJsonTree(this.federationJwk));
    jwks.add("keys", keys);
    return jwks;
  }
}
