package se.digg.oidfed.resolver;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Pair;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ResolverClient {
  private final Resolver resolver;
  private final String resolverIdentity;
  private final RSASSAVerifier verifier;
  private final Runnable treeReloadAction;
  private final Discovery discovery;

  public ResolverClient(final Resolver resolver, final String resolverIdentity, final JWK resolverKey,
      final Runnable treeReloadAction, final Discovery discovery) {
    try {
      this.resolver = resolver;
      this.resolverIdentity = resolverIdentity;
      this.verifier = new RSASSAVerifier(resolverKey.toRSAKey());
      this.treeReloadAction = treeReloadAction;
      this.discovery = discovery;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ResolverClientResponse resolve(final ResolverRequest request) {
    try {
      final SignedJWT jwt = SignedJWT.parse(resolver.resolve(request));
      jwt.verify(verifier);
      return new ResolverClientResponse(EntityStatement.parse(jwt));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String getResolverIdentity() {
    return resolverIdentity;
  }

  public void resolveNextTree() {
    this.treeReloadAction.run();
  }

  public class ResolverClientResponse {
    final EntityStatement entity;
    final List<Pair<Predicate<EntityStatement>, String>> assertions = new ArrayList<>();

    public ResolverClientResponse(final EntityStatement entity) {
      this.entity = entity;
    }

    public ResolverClientResponse withAssertion(final Predicate<EntityStatement> entityStatementPredicate,
        final String name) {
      this.assertions.add(Pair.of(entityStatementPredicate, name));
      return this;
    }

    public EntityStatement get() {
      final List<Pair<Predicate<EntityStatement>, String>> failedAssertions =
          this.assertions.stream().filter(p -> !p.getLeft().test(this.entity)).toList();

      if (!failedAssertions.isEmpty()) {
        final String message =
            "Failed to verify following:%s".formatted(failedAssertions.stream().map(Pair::getRight).toList());
        throw new IllegalArgumentException(message);
      }
      return entity;
    }
  }

  public Discovery getDiscovery() {
    return discovery;
  }
}
