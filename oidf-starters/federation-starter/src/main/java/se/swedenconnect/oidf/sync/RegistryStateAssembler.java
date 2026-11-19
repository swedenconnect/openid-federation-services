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
package se.swedenconnect.oidf.sync;

import com.nimbusds.jose.shaded.gson.JsonArray;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.JsonRegistryLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles entity records and module records from a {@link RegistrySyncState}, in the same json form as the
 * full responses from the registry, and parses them with the same {@link JsonRegistryLoader}.
 *
 * @author Felix Hellman
 */
@Slf4j
public class RegistryStateAssembler {

  private static final String ENTITY = "entity";
  private static final String TRUST_ANCHOR = "ta";
  private static final String TRUST_MARK_ISSUER = "tmi";
  private static final String RESOLVER = "resolver";
  private static final String SUBORDINATE_SEPARATOR = "|sub:";

  private final JsonRegistryLoader loader;

  /**
   * Constructor.
   * @param loader for parsing the assembled json
   */
  public RegistryStateAssembler(final JsonRegistryLoader loader) {
    this.loader = loader;
  }

  /**
   * @param state to assemble from
   * @return entity records, ordered by key
   */
  public List<EntityRecord> entityRecords(final RegistrySyncState state) {
    final JsonArray entities = new JsonArray();
    state.records().forEach((key, json) -> {
      if (ENTITY.equals(typeOf(key))) {
        entities.add(JsonParser.parseString(json));
      }
    });
    return this.loader.parseEntityRecord(entities.toString());
  }

  /**
   * Assembles the module record. A trust anchor is rebuilt from its {@code ta:<id>} record and all
   * {@code ta:<id>|sub:*} records, ordered by key.
   *
   * @param state to assemble from
   * @return module record
   */
  public ModuleRecord moduleRecord(final RegistrySyncState state) {
    final JsonArray resolvers = new JsonArray();
    final JsonArray trustMarkIssuers = new JsonArray();
    final Map<String, JsonObject> trustAnchors = new LinkedHashMap<>();
    final Map<String, JsonArray> subordinates = new HashMap<>();
    state.records().forEach((key, json) -> {
      switch (typeOf(key)) {
        case ENTITY -> {
        }
        case RESOLVER -> resolvers.add(JsonParser.parseString(json));
        case TRUST_MARK_ISSUER -> trustMarkIssuers.add(JsonParser.parseString(json));
        case TRUST_ANCHOR -> {
          final int separator = key.indexOf(SUBORDINATE_SEPARATOR);
          if (separator < 0) {
            trustAnchors.put(key, JsonParser.parseString(json).getAsJsonObject());
          }
          else {
            subordinates.computeIfAbsent(key.substring(0, separator), k -> new JsonArray())
                .add(JsonParser.parseString(json));
          }
        }
        default -> log.warn("Ignoring record with unknown key type from registry: {}", key);
      }
    });
    final JsonArray trustAnchorArray = new JsonArray();
    trustAnchors.forEach((key, trustAnchor) -> {
      final JsonArray subs = subordinates.remove(key);
      trustAnchor.remove("subordinates");
      if (subs != null) {
        trustAnchor.add("subordinates", subs);
      }
      trustAnchorArray.add(trustAnchor);
    });
    subordinates.keySet().forEach(key -> log.warn("Ignoring subordinates of missing trust anchor {}", key));
    final JsonObject root = new JsonObject();
    root.add("resolvers", resolvers);
    root.add("trust-anchors", trustAnchorArray);
    root.add("trust-mark-issuers", trustMarkIssuers);
    return this.loader.parseModuleJson(root.toString());
  }

  private static String typeOf(final String key) {
    final int separator = key.indexOf(':');
    return separator < 0 ? key : key.substring(0, separator);
  }
}
