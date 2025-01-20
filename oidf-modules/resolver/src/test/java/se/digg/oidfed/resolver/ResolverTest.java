package se.digg.oidfed.resolver;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicyEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.digg.oidfed.common.entity.EntityPathFactory;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.InMemoryEntityRecordRegistry;
import se.digg.oidfed.common.jwt.SignerFactory;
import se.digg.oidfed.resolver.metadata.MetadataFactory;
import se.digg.oidfed.resolver.metadata.MetadataPolicyFactory;
import se.digg.oidfed.resolver.tree.resolution.DeferredStepRecoveryStrategy;
import se.digg.oidfed.resolver.trustmark.TrustMarkFactory;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class ResolverTest {

  @Test
  void testResolveFederation() throws Exception {

    final MetadataFactory metadataFactory = new MetadataFactory();

    final String trustAnchorIdentifier = "http://trust-anchor.test";
    final String trustMarkIssuerIdentifier = "http://trust-mark-issuer.test";
    final String intermediateIdentifier = "http://intermediate.test";
    final String relyingPartyIdentifier = "http://relying-party.test";
    final String resolverIdentifier = "http://resolver.test";

    final List<MetadataPolicyEntry> policy = List.of(
        MetadataPolicyFactory.subset("grant_types", List.of("authorization_code", "refresh_token")),
        MetadataPolicyFactory.subset("token_endpoint_auth_method", List.of("private_key_jwt"))
    );

    final MetadataPolicy metadataPolicy = new MetadataPolicy();

    policy.forEach(metadataPolicy::put);

    /*
     * Trust Anchor Signs itself with its own key
     * location: http://trust-anchor.test/.well-known/openid-federation
     */
    final FederationEntity trustAnchorEntity = new FederationEntity.Builder()
        .issuer(trustAnchorIdentifier)
        .subject(trustAnchorIdentifier)
        .customize(c -> c.claim("metadata", metadataFactory.trustAnchor(trustAnchorIdentifier)))
        .customize(c -> c.claim("metadata_policy", Map.of("openid_relying_party", metadataPolicy.toJSONObject())))
        .customize(c -> c.claim("trust_mark_issuer", trustMarkIssuerIdentifier))
        .build();

    /*
     * LEAF
     * Trust Mark Issuer
     * location: http://trust-mark-issuer.test/.well-known/openid-federeation
     */
    final FederationEntity trustMarkIssuer = new FederationEntity.Builder()
        .issuer(trustMarkIssuerIdentifier)
        .subject(trustMarkIssuerIdentifier)
        .customize(c -> c.claim("authority_hints", List.of(trustAnchorIdentifier)))
        .customize(c -> c.claim("metadata", metadataFactory.federationEntity()))
        .build();

    /*
     * LEAF
     * Relying party Entity Signs itself with its own key
     * location: http://relying-party.test/.well-known/openid-federation
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

    final FederationEntity intermediateAuthority = new FederationEntity.Builder()
        .issuer(intermediateIdentifier)
        .subject(intermediateIdentifier)
        .customize(c -> c.claim("metadata", metadataFactory.trustAnchor(intermediateIdentifier)))
        .build();

    /*
     * Intermediate Authority Subject Statement is signed by its own key but trusts the leaf entity key
     * location: http://federation.se/intermediate/fetch?sub=http://relying-party.test
     */
    final FederationEntity intermediateAuthoritySubjectStatement = new FederationEntity.Builder()
        .issuer(intermediateIdentifier)
        .subject(relyingPartyIdentifier)
        .signKey(intermediateAuthority.getSignKey())
        .trustedKey(relyingPartyEntity.getSignKey())
        .build();

    /*
     * The Trust Anchor Subject Statement is signed by the TA sign key but trusts the intermediate sign key
     * location: http://federation.se/fetch?sub=http://intermediate.test
     */
    final FederationEntity trustAnchorSubjectStatement = new FederationEntity.Builder()
        .issuer(trustAnchorIdentifier)
        .subject(intermediateIdentifier)
        .signKey(trustAnchorEntity.getSignKey())
        .trustedKey(intermediateAuthoritySubjectStatement.getSignKey())
        .build();

    /*
     * The Trust Anchor Subject Statement about the Trust Mark Issuer is signed by the TA sign key but trusts the Trust Mark Issuer Key
     * location: https://federation.se/intermediate/fetch?sub=http://relying-party.test
     */
    final FederationEntity trustAnchorTrustMarkIssuerSubjectStatement = new FederationEntity.Builder()
        .issuer(trustAnchorIdentifier)
        .subject(trustMarkIssuerIdentifier)
        .signKey(trustAnchorEntity.getSignKey())
        .trustedKey(trustMarkIssuer.getSignKey())
        .customize(c -> c.claim("subject_entity_configuration_location", "data:application/entity-statement+jwt,%s".formatted(trustMarkIssuer.getSignedJwt())))
        .build();

    final FederationTree tree = new FederationTree(trustAnchorEntity);

    tree.addChild(trustAnchorSubjectStatement, trustAnchorEntity);
    tree.addChild(trustAnchorTrustMarkIssuerSubjectStatement, trustAnchorEntity);
    //We do not add trustAnchorTrustMarkIssuerSubjectStatement to trustMarkIssuer since it is embedded.

    tree.addChild(intermediateAuthority, trustAnchorSubjectStatement);
    tree.addChild(intermediateAuthoritySubjectStatement, intermediateAuthority);
    tree.addChild(relyingPartyEntity, intermediateAuthoritySubjectStatement);

    final JWKSet allKeys = tree.findAllKeys();

    Assertions.assertEquals(4, allKeys.getKeys().stream().distinct().toList().size(),
        "The federation should contain four unique keys");

    final JWK resolverKey = new RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .issueTime(new Date())
        .generate();

    final ResolverProperties properties =
        new ResolverProperties(trustAnchorIdentifier, Duration.ZERO, tree.findAllKeys().getKeys(), resolverIdentifier
            , Duration.ofSeconds(10), "alias");
    final DeferredStepRecoveryStrategy deferredStepRecoveryStrategy = new DeferredStepRecoveryStrategy();

    //Fail relyingparty first time we try it
    tree.setFailOnce(relyingPartyIdentifier + "/.well-known/openid-federation");
    tree.setFailOnce(trustMarkIssuerIdentifier + "/.well-known/openid-federation");
    final JWKSet jwks = new JWKSet(List.of(resolverKey));
    final SignerFactory adapter = new SignerFactory(jwks);
    final ResolverClient resolver = ResolverFactory.createTestResolver(properties, tree, deferredStepRecoveryStrategy
        , adapter);
    //Re-run resolution of the steps that failed
    deferredStepRecoveryStrategy.retry();
    final ResolverRequest request = new ResolverRequest(relyingPartyIdentifier, trustAnchorIdentifier, null);
    resolver.resolve(request)
        .withAssertion(statement -> resolver.getResolverIdentity()
            .equalsIgnoreCase(statement.getClaimsSet().getIssuer().getValue()), "Issuer should be resolver")
        .withAssertion(
            statement -> !((List<String>) statement.getClaimsSet().getMetadata(EntityType.OPENID_RELYING_PARTY)
                .get("grant_types")).contains("client_credentials"),
            "Client Credential should be missing but is present")
        .withAssertion(
            statement -> ((List<String>) statement.getClaimsSet().getMetadata(EntityType.OPENID_RELYING_PARTY)
                .get("grant_types")).contains("authorization_code"), "Authorization Code should be present")
        .get();

    final ResolverRequest trustMarkIssuerResolveRequest =
        new ResolverRequest(trustMarkIssuerIdentifier, trustAnchorIdentifier, null);
    resolver.resolve(trustMarkIssuerResolveRequest)
        .withAssertion(
            entity -> resolver.getResolverIdentity().equalsIgnoreCase(entity.getClaimsSet().getIssuer().getValue()),
            "Issuer should be resovler")
        .get();

    resolver.resolveNextTree();

    resolver.resolve(trustMarkIssuerResolveRequest)
        .withAssertion(
            entity -> resolver.getResolverIdentity().equalsIgnoreCase(entity.getClaimsSet().getIssuer().getValue()),
            "Issuer should be resovler")
        .get();

    final DiscoveryResponse discovery =
        resolver.discovery(new DiscoveryRequest(trustAnchorIdentifier, null, null));
    Assertions.assertEquals(4, discovery.supportedEntities().size());
    Assertions.assertTrue(List.of(trustAnchorIdentifier, intermediateIdentifier, trustMarkIssuerIdentifier, relyingPartyIdentifier).containsAll(discovery.supportedEntities()));
  }
}