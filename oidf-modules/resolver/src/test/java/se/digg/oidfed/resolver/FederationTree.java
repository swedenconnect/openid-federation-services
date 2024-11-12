package se.digg.oidfed.resolver;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import se.digg.oidfed.common.tree.CacheSnapshot;
import se.digg.oidfed.common.tree.Node;
import se.digg.oidfed.common.tree.SearchRequest;
import se.digg.oidfed.common.tree.Tree;
import se.digg.oidfed.common.tree.VersionedInMemoryCache;
import se.digg.oidfed.resolver.integration.EntityStatementIntegration;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class FederationTree implements EntityStatementIntegration {
  private final VersionedInMemoryCache<FederationEntity>
      federationEntityInMemoryDataLayer = new VersionedInMemoryCache<>();
  private final Tree<FederationEntity> federationEntityTree = new Tree<>(federationEntityInMemoryDataLayer);
  private final Set<String> failedLocations = new ConcurrentSkipListSet<>();


  public FederationTree(final FederationEntity entity) {
    final Node<FederationEntity> node = new Node<>(entity.getLocation());
    federationEntityTree.addRoot(node, entity);
    federationEntityInMemoryDataLayer.useNextVersion();
  }

  public void addChild(final FederationEntity entity, final FederationEntity parent) {
    final Node<FederationEntity> node = new Node<>(entity.getLocation());
    federationEntityTree.addChild(node, parent.getLocation(), entity, federationEntityTree.getCurrentSnapshot());
  }

  public JWKSet findAllKeys() {
    final CacheSnapshot<FederationEntity> snapshot = federationEntityTree.getCurrentSnapshot();
    final List<JWK> jwks = federationEntityTree.search(new SearchRequest<>((n, c) -> true, false, snapshot)).stream()
        .map(result -> snapshot.getData(result.node().getKey()).getSelfKey())
        .toList();

    return new JWKSet(jwks);
  }

  @Override
  public EntityStatement getEntityStatement(final String location) {
    if (failedLocations.remove(location)) {
      throw new RuntimeException("Failed to fetch statement because of test specification");
    }
    try {
      if (location.contains("data:application/entity-statement+jwt")) {
        return EntityStatement.parse(location.split(",")[1]);
      }
      final FederationEntity entity = findEntity(location);
      return EntityStatement.parse(entity.getSignedJwt());
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private FederationEntity findEntity(final String location) {
    final CacheSnapshot<FederationEntity> snapshot = federationEntityTree.getCurrentSnapshot();
    return federationEntityTree
        .search(new SearchRequest<>((n, c) -> n.getLocation().equalsIgnoreCase(location), false, snapshot))
        .stream()
        .map(result -> snapshot.getData(result.node().getKey()))
        .findFirst()
        .get();
  }

  @Override
  public List<String> getSubordinateListing(final String location) {
    try {
      //Commit to a tree version when executing the search
      final CacheSnapshot<FederationEntity> snapshot = federationEntityTree.getCurrentSnapshot();

      return federationEntityTree
          .search(new SearchRequest<>((n, c) -> n.getListLocation().equalsIgnoreCase(location), false, snapshot))
          .stream()
          .flatMap(result -> snapshot.getChildren(result.node()).stream()
              .map(child -> snapshot.getData(child.getKey()).getSubject()))
          .toList();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Fails the mocked request to fail once
   * @param location
   */
  public void setFailOnce(final String location) {
    this.failedLocations.add(location);
  }
}
