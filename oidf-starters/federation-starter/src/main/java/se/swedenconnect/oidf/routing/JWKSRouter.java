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
package se.swedenconnect.oidf.routing;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.jwt.JWKFederationSigner;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;

import java.util.Map;
import java.util.Optional;

/**
 * Router for displaying public jwks for this node.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class JWKSRouter implements Router {

  private final KeyRegistry registry;

  @Override
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET("/jwks", handler -> {
      final JWKFederationSigner signer = new JWKFederationSigner(this.registry.getDefaultKey());
      final Map<String, JWKSet> mappedPublicKeys = this.registry.getMappedPublicKeys();
      final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
      Optional.ofNullable(mappedPublicKeys.get("federation")).ifPresent(fed -> {
            builder.claim("federation", fed.toPublicJWKSet().toJSONObject() );
          }
      );
      Optional.ofNullable(mappedPublicKeys.get("hosted")).ifPresent(hosted -> {
            builder.claim("hosted", hosted.toPublicJWKSet().toJSONObject() );
          }
      );
      final SignedJWT signedJwt = signer.sign(JOSEObjectType.JWT, builder.build());
      return ServerResponse.ok().body(signedJwt);
    });
  }
}
