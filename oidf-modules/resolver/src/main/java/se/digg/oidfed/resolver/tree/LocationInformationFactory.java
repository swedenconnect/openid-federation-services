/*
 * Copyright 2024-2025 Sweden Connect
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
package se.digg.oidfed.resolver.tree;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import net.minidev.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;

/**
 * Factory class for determining location of subjects.
 *
 * @author Felix Hellman
 */
public class LocationInformationFactory {
  /**
   * @param entityStatement to check
   * @return authorityInformation if the {@link EntityStatement} is an authority, otherwise {@link Optional#empty()}
   */
  public static Optional<AuthorityInformation> getAuthorityInformation(final EntityStatement entityStatement) {
    final JSONObject metadata = entityStatement.getClaimsSet().getMetadata(EntityType.FEDERATION_ENTITY);
    if (!entityStatement.getClaimsSet().isSelfStatement()) {
      return Optional.empty();
    }
    if (Objects.nonNull(metadata) && metadata.containsKey("federation_list_endpoint")) {
      final String federationListEndpoint = (String) metadata.get("federation_list_endpoint");
      final String federationFetchEndpoint = (String) metadata.get("federation_fetch_endpoint");
      return Optional.of(new AuthorityInformation(federationListEndpoint, federationFetchEndpoint));
    }
    return Optional.empty();
  }

  /**
   * @param entityStatement to check
   * @return subject information if the {@link EntityStatement} is a subject, otherwise {@link Optional#empty()}
   */
  public static Optional<SubjectInformation> getSubjectInformation(final EntityStatement entityStatement) {
    if (!entityStatement.getClaimsSet().isSelfStatement()) {
      final String subject = entityStatement.getClaimsSet().getSubject().getValue();
      final String location = "%s/.well-known/openid-federation".formatted(subject);
      final Optional<String> configurationLocation = Optional.ofNullable(entityStatement.getClaimsSet()
              .getClaim("subject_entity_configuration_location"))
              .map(String.class::cast);
      return Optional.of(new SubjectInformation(location, configurationLocation));
    }
    return Optional.empty();
  }

  /**
   * @param listEndpoint list endpoint for an authority
   * @param fetchEndpoint fetch endpoint for an authority
   */
  public record AuthorityInformation(String listEndpoint, String fetchEndpoint) {
    /**
     * @param entityId of a subject that belongs to the authority
     * @return fetch endpoint for a given subject
     */
    public String subjectFetchEndpoint(final String entityId) {
      return "%s?sub=%s".formatted(fetchEndpoint(), URLEncoder.encode(entityId, Charset.defaultCharset()));
    }
  }

  /**
   * @param location of a given subject
   * @param configurationLocation value of optional claim 'subject_entity_configuration_location'
   */
  public record SubjectInformation(String location, Optional<String> configurationLocation) {}
}
