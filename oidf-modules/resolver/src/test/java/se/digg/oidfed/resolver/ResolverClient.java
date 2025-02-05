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

package se.digg.oidfed.resolver;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Pair;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import se.digg.oidfed.common.entity.integration.federation.ResolveRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ResolverClient {
  private final Resolver resolver;
  private final String resolverIdentity;
  private final RSASSAVerifier verifier;
  private final Runnable treeReloadAction;

  public ResolverClient(final Resolver resolver, final String resolverIdentity, final JWK resolverKey,
      final Runnable treeReloadAction) {
    try {
      this.resolver = resolver;
      this.resolverIdentity = resolverIdentity;
      this.verifier = new RSASSAVerifier(resolverKey.toRSAKey());
      this.treeReloadAction = treeReloadAction;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ResolverClientResponse resolve(final ResolveRequest request) {
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

  public DiscoveryResponse discovery(final DiscoveryRequest discoveryRequest) {
    return this.resolver.discovery(discoveryRequest);
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
}
