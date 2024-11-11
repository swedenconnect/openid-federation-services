package se.digg.oidfed.service.resolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Body;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicyEntry;
import se.digg.oidfed.test.FederationEntity;
import se.digg.oidfed.test.metadata.MetadataFactory;
import se.digg.oidfed.test.metadata.MetadataPolicyFactory;
import se.digg.oidfed.test.trustmark.TrustMarkFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WiremockFederation {

  private static final ObjectMapper mapper = new ObjectMapper();

  public static void configureEntity(final FederationEntity entity, final List<String> entityChildren)
      throws JsonProcessingException {
    if (entity.getIssuer().equalsIgnoreCase(entity.getSubject())) {
      final String selfLocation = entity.getLocation().split("9090")[1];
      final String listLocation = entity.getListLocation().split("9090")[1];
      WireMock.stubFor(WireMock.get(selfLocation)
          .willReturn(new ResponseDefinitionBuilder().withResponseBody(new Body(entity.getSignedJwt()))));
      WireMock.stubFor(WireMock.get(listLocation).willReturn(
          new ResponseDefinitionBuilder().withResponseBody(new Body(mapper.writeValueAsString(entityChildren)))));
    }
    else {
      final String[] split = entity.getLocation().split("http://localhost:9090");
      final String selfLocation = split[1];
      WireMock.stubFor(WireMock.get(selfLocation)
          .willReturn(new ResponseDefinitionBuilder().withResponseBody(new Body(entity.getSignedJwt()))));
    }
  }

  public static List<FederationEntity> configure(int port) throws JsonProcessingException {
    final MetadataFactory metadataFactory = new MetadataFactory();

    final String trustAnchorIdentifier = "http://localhost:%d/trustanchor".formatted(port);
    final String trustMarkIssuerIdentifier = "http://localhost:%d/trustMarkIssuer".formatted(port);
    final String intermediateIdentifier = "http://localhost:%d/intermediate".formatted(port);
    final String relyingPartyIdentifier = "http://localhost:%d/intermediate/relyingparty".formatted(port);

    final List<MetadataPolicyEntry> policy = List.of(
        MetadataPolicyFactory.subset("grant_types", List.of("authorization_code", "refresh_token")),
        MetadataPolicyFactory.subset("token_endpoint_auth_method", List.of("private_key_jwt"))
    );

    final MetadataPolicy metadataPolicy = new MetadataPolicy();

    policy.forEach(metadataPolicy::put);

    /*
     * Trust Anchor Signs itself with its own key
     * location: http://trust-anchor.test/.well-known/openid-federation
     * children: [http://federation.se/fetch?sub=http://trust-mark-issuer, http://federation.se/fetch?sub=http://intermediate.test]
     */
    final FederationEntity trustAnchorEntity = new FederationEntity.Builder()
        .issuer(trustAnchorIdentifier)
        .subject(trustAnchorIdentifier)
        .customize(c -> c.claim("metadata", metadataFactory.trustAnchor(trustAnchorIdentifier)))
        .customize(c -> c.claim("metadata_policy", Map.of("openid_relying_party", metadataPolicy.toJSONObject())))
        .customize(c -> c.claim("trust_mark_issuer", trustMarkIssuerIdentifier))
        .build();

    configureEntity(trustAnchorEntity, List.of(intermediateIdentifier));

    /*
     * LEAF
     * Trust Mark Issuer
     * location: http://trust-mark-issuer.test/.well-known/openid-federeation
     * children: []
     */
    final FederationEntity trustMarkIssuer = new FederationEntity.Builder()
        .issuer(trustMarkIssuerIdentifier)
        .subject(trustMarkIssuerIdentifier)
        .customize(c -> c.claim("authority_hints", List.of(trustAnchorIdentifier)))
        .customize(c -> c.claim("metadata", metadataFactory.federationEntity()))
        .build();

    configureEntity(trustMarkIssuer, List.of());

    /*
     * LEAF
     * Relying party Entity Signs itself with its own key
     * location: http://relying-party.test/.well-known/openid-federation
     * children: []
     */
    final String leafEntityTrustMark =
        TrustMarkFactory.createTrustMark(trustMarkIssuerIdentifier, relyingPartyIdentifier,
            trustMarkIssuer.getSignKey());
    final FederationEntity relyingPartyEntity = new FederationEntity.Builder()
        .issuer(relyingPartyIdentifier)
        .subject(relyingPartyIdentifier)
        .customize(c -> {
          final Map<String, Object> metadata = new HashMap<>(metadataFactory.relyingParty());
          metadata.putAll(metadataFactory.federationEntity());
          c.claim("metadata", metadata);
        })
        .customize(c -> c.claim("trust_marks",
            List.of(Map.of("id", relyingPartyIdentifier, "trust_mark", leafEntityTrustMark))))
        .customize(c -> c.claim("authotity_hints", List.of(intermediateIdentifier)))
        .build();

    configureEntity(relyingPartyEntity, List.of());

    final FederationEntity intermediateAuthority = new FederationEntity.Builder()
        .issuer(intermediateIdentifier)
        .subject(intermediateIdentifier)
        .customize(c -> c.claim("metadata", metadataFactory.trustAnchor(intermediateIdentifier)))
        .build();

    configureEntity(intermediateAuthority, List.of(relyingPartyIdentifier));


    /*
     * Intermediate Authority Subject Statement is signed by its own key but trusts the leaf entity key
     * location: http://federation.se/intermediate/fetch?sub=http://relying-party.test
     * children: [http://relying-party.test/.well-known/openid-federation]
     */
    final FederationEntity intermediateAuthoritySubjectStatement = new FederationEntity.Builder()
        .issuer(intermediateIdentifier)
        .subject(relyingPartyIdentifier)
        .signKey(intermediateAuthority.getSignKey())
        .trustedKey(relyingPartyEntity.getSignKey())
        .build();

    configureEntity(intermediateAuthoritySubjectStatement, List.of());

    /*
     * The Trust Anchor Subject Statement is signed by the TA sign key but trusts the intermediate sign key
     * location: http://federation.se/fetch?sub=http://intermediate.test
     * children: []
     */
    final FederationEntity trustAnchorSubjectStatement = new FederationEntity.Builder()
        .issuer(trustAnchorIdentifier)
        .subject(intermediateIdentifier)
        .signKey(trustAnchorEntity.getSignKey())
        .trustedKey(intermediateAuthoritySubjectStatement.getSignKey())
        .build();

    configureEntity(trustAnchorSubjectStatement, List.of());

    final FederationEntity trustAnchorTrustMarkIssuerSubjectStatement = new FederationEntity.Builder()
        .issuer(trustAnchorIdentifier)
        .subject(trustMarkIssuerIdentifier)
        .signKey(trustAnchorEntity.getSignKey())
        .trustedKey(trustMarkIssuer.getSignKey())
        .build();

    configureEntity(trustAnchorTrustMarkIssuerSubjectStatement, List.of());

    return List.of(
        trustAnchorEntity,
        trustAnchorSubjectStatement,
        trustMarkIssuer,
        trustAnchorTrustMarkIssuerSubjectStatement,
        relyingPartyEntity,
        intermediateAuthority,
        intermediateAuthoritySubjectStatement
    );
  }
}
