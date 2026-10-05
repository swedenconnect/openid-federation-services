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
package se.swedenconnect.oidf.resolver.chain;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates where claims are placed and their syntax, for every statement in the chain (OpenID Federation 1.0,
 * Section 3.2, steps 14 to 23). Claims that only belong in Entity Configurations, or only in Subordinate Statements,
 * make the statement invalid when found in the other kind. The trust mark entries in {@code trust_marks} are not
 * checked here; an invalid entry is skipped when trust marks are collected.
 *
 * @author Martin Lindström
 */
public class StatementSyntaxValidationStep implements ChainValidationStep {

  private static final List<String> ENTITY_CONFIGURATION_CLAIMS = List.of(
      "authority_hints", "trust_anchor_hints", "trust_marks", "trust_mark_issuers", "trust_mark_owners");

  private static final List<String> SUBORDINATE_STATEMENT_CLAIMS = List.of(
      "metadata_policy", "metadata_policy_crit", "constraints", "source_endpoint");

  @Override
  public List<ChainValidationError> validate(final List<SignedJWT> chain) {
    final List<ChainValidationError> errors = new ArrayList<>();
    for (final SignedJWT statement : chain) {
      try {
        final JWTClaimsSet claims = statement.getJWTClaimsSet();
        final String error = check(claims);
        if (error != null) {
          errors.add(new ChainValidationError(ChainValidationErrorType.MALFORMED_STATEMENT,
              "Statement issued by %s about %s: %s".formatted(claims.getIssuer(), claims.getSubject(), error),
              null));
        }
      }
      catch (final ParseException e) {
        errors.add(new ChainValidationError(ChainValidationErrorType.MALFORMED_STATEMENT,
            "Statement claims cannot be parsed: " + e.getMessage(), e));
      }
    }
    return errors;
  }

  private static String check(final JWTClaimsSet claims) {
    final Map<String, Object> all = claims.getClaims();
    final boolean entityConfiguration = claims.getIssuer() != null && claims.getIssuer().equals(claims.getSubject());
    final List<String> notAllowed = entityConfiguration ? SUBORDINATE_STATEMENT_CLAIMS : ENTITY_CONFIGURATION_CLAIMS;
    for (final String claim : notAllowed) {
      if (all.containsKey(claim)) {
        return "%s is not allowed in %s".formatted(claim,
            entityConfiguration ? "an Entity Configuration" : "a Subordinate Statement");
      }
    }
    if (all.containsKey("authority_hints")) {
      if (!isStringArray(all.get("authority_hints")) || ((List<?>) all.get("authority_hints")).isEmpty()) {
        return "authority_hints must be a non-empty array of entity identifiers";
      }
    }
    if (all.containsKey("trust_anchor_hints") && !isStringArray(all.get("trust_anchor_hints"))) {
      return "trust_anchor_hints must be an array of entity identifiers";
    }
    if (all.containsKey("metadata")) {
      final String error = checkMetadata(all.get("metadata"));
      if (error != null) {
        return error;
      }
    }
    if (all.containsKey("metadata_policy") && !isObjectOfObjectsOfObjects(all.get("metadata_policy"))) {
      return "metadata_policy must map entity types to parameter policies";
    }
    if (all.containsKey("constraints")) {
      final String error = checkConstraints(all.get("constraints"));
      if (error != null) {
        return error;
      }
    }
    if (all.containsKey("trust_marks") && !(all.get("trust_marks") instanceof List<?>)) {
      return "trust_marks must be an array";
    }
    if (all.containsKey("trust_mark_issuers")) {
      if (!(all.get("trust_mark_issuers") instanceof final Map<?, ?> issuers)
          || !issuers.values().stream().allMatch(StatementSyntaxValidationStep::isStringArray)) {
        return "trust_mark_issuers must map trust mark types to arrays of entity identifiers";
      }
    }
    if (all.containsKey("trust_mark_owners")) {
      final String error = checkTrustMarkOwners(all.get("trust_mark_owners"));
      if (error != null) {
        return error;
      }
    }
    if (all.containsKey("source_endpoint") && !isUrl(all.get("source_endpoint"))) {
      return "source_endpoint must be a URL";
    }
    return null;
  }

  private static String checkMetadata(final Object metadata) {
    if (!(metadata instanceof final Map<?, ?> types)) {
      return "metadata must be a JSON object";
    }
    for (final Map.Entry<?, ?> type : types.entrySet()) {
      if (!(type.getValue() instanceof final Map<?, ?> parameters)) {
        return "metadata for %s must be a JSON object".formatted(type.getKey());
      }
      for (final Map.Entry<?, ?> parameter : parameters.entrySet()) {
        if (parameter.getValue() == null) {
          return "metadata parameter %s.%s must not be null".formatted(type.getKey(), parameter.getKey());
        }
      }
    }
    return null;
  }

  private static String checkConstraints(final Object constraints) {
    if (!(constraints instanceof final Map<?, ?> values)) {
      return "constraints must be a JSON object";
    }
    if (values.containsKey("max_path_length")
        && !(values.get("max_path_length") instanceof final Number length && length.longValue() >= 0
        && length.doubleValue() == Math.floor(length.doubleValue()))) {
      return "max_path_length must be an integer of at least zero";
    }
    if (values.containsKey("naming_constraints")) {
      if (!(values.get("naming_constraints") instanceof final Map<?, ?> naming)
          || naming.containsKey("permitted") && !isStringArray(naming.get("permitted"))
          || naming.containsKey("excluded") && !isStringArray(naming.get("excluded"))) {
        return "naming_constraints must hold arrays of names in permitted and excluded";
      }
    }
    if (values.containsKey("allowed_entity_types") && !isStringArray(values.get("allowed_entity_types"))) {
      return "allowed_entity_types must be an array of entity types";
    }
    return null;
  }

  private static String checkTrustMarkOwners(final Object owners) {
    if (!(owners instanceof final Map<?, ?> types)) {
      return "trust_mark_owners must be a JSON object";
    }
    for (final Object owner : types.values()) {
      if (!(owner instanceof final Map<?, ?> values) || !(values.get("sub") instanceof String)
          || !(values.get("jwks") instanceof Map<?, ?>)) {
        return "trust_mark_owners entries must hold sub and jwks";
      }
      try {
        @SuppressWarnings("unchecked")
        final Map<String, Object> jwks = (Map<String, Object>) values.get("jwks");
        JWKSet.parse(jwks);
      }
      catch (final ParseException e) {
        return "trust_mark_owners jwks is not a valid JWK set";
      }
    }
    return null;
  }

  private static boolean isStringArray(final Object value) {
    return value instanceof final List<?> list && list.stream().allMatch(String.class::isInstance);
  }

  private static boolean isObjectOfObjectsOfObjects(final Object value) {
    return value instanceof final Map<?, ?> types
        && types.values().stream().allMatch(parameters -> parameters instanceof final Map<?, ?> policies
        && policies.values().stream().allMatch(Map.class::isInstance));
  }

  private static boolean isUrl(final Object value) {
    if (!(value instanceof final String url)) {
      return false;
    }
    try {
      final URI uri = new URI(url);
      return uri.getScheme() != null && uri.getHost() != null;
    }
    catch (final URISyntaxException e) {
      return false;
    }
  }
}
