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
package se.digg.oidfed.common.entity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class EntityRecordTest {

  @Test
  void serialize_fromJson() throws JOSEException, ParseException {
    final RSAKey key = new RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .issueTime(new Date())
        .generate();

    final EntityRecord expected = new EntityRecord(
        new EntityID("http://issuer.test"),
        new EntityID("http://subject.test"),
        "my-awesome-policy",
        new JWKSet(List.of(key)),
        "http://override.test/location",
        null);

    final RSASSASigner signer = new RSASSASigner(key);
    final RSASSAVerifier verifier = new RSASSAVerifier(key);
    final EntityRecordVerifier entityRecordVerifier = new EntityRecordVerifier(new JWKSet(List.of(key)));
    final EntityRecordSigner entityRecordSigner = new EntityRecordSigner(signer);
    final SignedJWT serialize = entityRecordSigner.signRecords(List.of(expected));
    System.out.println(serialize.serialize());
    final EntityRecord actual = entityRecordVerifier.verify(serialize).getFirst();

    Assertions.assertEquals(expected.getSubject(), actual.getSubject());
    Assertions.assertEquals(expected.getIssuer(), actual.getIssuer());
    Assertions.assertTrue(expected.getJwks().containsJWK(actual.getJwks().getKeys().get(0)));
    Assertions.assertEquals(expected.getPolicyRecordId(), actual.getPolicyRecordId());
    Assertions.assertEquals(expected.getOverrideConfigurationLocation(), actual.getOverrideConfigurationLocation());
  }

  @Test
  void serialize_fromJson_with_hosted() throws JOSEException, ParseException {
    final RSAKey key = new RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .issueTime(new Date())
        .generate();


    final TrustMarkSource trustMarkSource = new TrustMarkSource(new EntityID("http://trustmarkissuer.test"), "http://trustmark.test/1");
    final Map<String, Object> metadata = Map.of("metadata", Map.of("key", "value"));

    final HostedRecord hosted = HostedRecord.builder()
        .metadata(metadata)
        .trustMarkSources(List.of(trustMarkSource))
        .authorityHints(List.of())
        .build();

    final EntityRecord expected = EntityRecord.builder()
        .issuer(new EntityID("http://issuer.test"))
        .subject(new EntityID("http://subject.test"))
        .jwks(new JWKSet(List.of(key)))
        .policyRecordId("my-awesome-policy")
        .hostedRecord(hosted)
        .build();

    final RSASSASigner signer = new RSASSASigner(key);
    final EntityRecordVerifier entityRecordVerifier = new EntityRecordVerifier(new JWKSet(List.of(key)));
    final EntityRecordSigner entityRecordSigner = new EntityRecordSigner(signer);
    final SignedJWT signedJWT = entityRecordSigner.signRecords(List.of(expected));
    System.out.println(signedJWT.serialize());
    final EntityRecord actual = entityRecordVerifier.verify(signedJWT).getFirst();

    Assertions.assertEquals(expected.getSubject(), actual.getSubject());
    Assertions.assertEquals(expected.getIssuer(), actual.getIssuer());
    Assertions.assertTrue(expected.getJwks().containsJWK(actual.getJwks().getKeys().get(0)));
    Assertions.assertEquals(expected.getHostedRecord().getMetadata().get("key"),
        actual.getHostedRecord().getMetadata().get("key"));
    final TrustMarkSource expectedTrustMarkSource = expected.getHostedRecord().getTrustMarkSources().get(0);
    final TrustMarkSource actualTrustMarkSource = actual.getHostedRecord().getTrustMarkSources().get(0);
    Assertions.assertEquals(expectedTrustMarkSource.getTrustMarkId(), actualTrustMarkSource.getTrustMarkId());
    Assertions.assertEquals(expectedTrustMarkSource.getIssuer().getValue(), actualTrustMarkSource.getIssuer().getValue());
    Assertions.assertEquals(expected.getPolicyRecordId(), actual.getPolicyRecordId());
    Assertions.assertEquals(expected.getHostedRecord().getAuthorityHints(), actual.getHostedRecord().getAuthorityHints());
  }
}