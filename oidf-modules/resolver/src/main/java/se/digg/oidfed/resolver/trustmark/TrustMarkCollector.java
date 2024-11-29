/*
 * Copyright 2024 Sweden Connect
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
package se.digg.oidfed.resolver.trustmark;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.List;

/**
 * Responsible for collecting trust marks from a trust chain.
 *
 * @author Felix Hellman
 */
public class TrustMarkCollector {
  /**
   * @param chain to check
   * @return trust marks from chain
   */
  public static List<TrustMarkEntry> collectSubjectTrustMarks(final List<EntityStatement> chain) {
    final EntityStatement leafStatement = chain.getFirst();
    if (leafStatement.getClaimsSet().getTrustMarks() == null) {
      return List.of();
    }
    final EntityStatement superiorStatement = chain.get(2);
    final String subject = leafStatement.getClaimsSet().getSubject().getValue();

    leafStatement.getClaimsSet().getJSONArrayClaim("trust_marks");
    final List<TrustMarkEntry> trustMarks = TrustMarkCollector.parseTrustmark(leafStatement);
    if (superiorStatement.getClaimsSet().getSubject().getValue().equals(subject)) {
      // If the superior statement is issued for the subject,
      // then collect any trust marks not present in the leaf statement
      final List<TrustMarkEntry> superiorStatementTrustMarks = TrustMarkCollector.parseTrustmark(superiorStatement);
      superiorStatementTrustMarks.stream()
          .filter(supTrustMark -> trustMarks.stream()
              .noneMatch(subjTrustMark -> supTrustMark.getID().equals(subjTrustMark.getID())))
          .forEach(trustMarks::add);
    }
    return trustMarks;
  }

  private static List<TrustMarkEntry> parseTrustmark(final EntityStatement entity) {
    final JSONArray trustMarks = entity.getClaimsSet().getJSONArrayClaim("trust_marks");
    return trustMarks.stream().toList().stream()
        .map(JSONObject.class::cast)
        .map(json -> {
          try {
            return TrustMarkEntry.parse(json);
          }
          catch (final ParseException e) {
            throw new IllegalArgumentException("Failed to parse TrustMarkEntry", e);
          }
        }).toList();
  }
}
