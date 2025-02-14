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
package se.digg.oidfed.service.entity;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Body;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.core.io.ClassPathResource;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.HostedRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;
import se.digg.oidfed.common.entity.integration.registry.ModuleResponse;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkSubjectRecord;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

import static se.digg.oidfed.service.IntegrationTestParent.RP_FROM_REGISTRY_ENTITY;

public class RegistryMock {

  private final RegistryRecordSigner registryRecordSigner;

  public RegistryMock() throws JOSEException, KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
    final KeyStore keystore = KeyStore.getInstance(new ClassPathResource("signkey.p12").getFile(), "changeit".toCharArray());
    final JWKSet set = new JWKSet(JWK.load(
        keystore,
        "1",
        "changeit".toCharArray()
    ));

    registryRecordSigner = new RegistryRecordSigner(new RSASSASigner(set.getKeys().getFirst().toRSAKey()));
  }


  public void init(final String instanceId) throws Exception {
    final WireMockServer wireMockServer = new WireMockServer(9090);
    wireMockServer.start();
    WireMock.configureFor(9090);


    final String policyId = "my-super-policy";
    final List<EntityRecord> municipalityEntities = List.of(
        EntityRecord.builder()
            .issuer(TestFederationEntities.Municipality.TRUST_ANCHOR)
            .subject(RP_FROM_REGISTRY_ENTITY)
            .policyRecordId(policyId)
            .hostedRecord(HostedRecord.builder().metadata(Map.of("federation_entity", Map.of("organization_name",
                "Municipality"))).build())
            .build()
    );


    mockModuleFor(new ModuleResponse(), instanceId);

    mockRecordsFor(municipalityEntities, TestFederationEntities.Municipality.TRUST_ANCHOR);
    mockRecordsFor(List.of(), TestFederationEntities.Authorization.TRUST_ANCHOR);
    mockRecordsFor(List.of(), TestFederationEntities.PrivateSector.TRUST_ANCHOR);
    mockRecordsFor(List.of(), TestFederationEntities.Authorization.TRUST_MARK_ISSUER);
    mockRecordsFor(List.of(), TestFederationEntities.Municipality.RESOLVER);

    final PolicyRecord record = new PolicyRecord(policyId, Map.of());
    final String policyBody = registryRecordSigner.signPolicy(record).serialize();
    createPolicy(policyBody, policyId);
    final List<TrustMarkSubjectRecord> tms = List.of(
        new TrustMarkSubjectRecord(TestFederationEntities.Authorization.OP_1.getValue(),
            TestFederationEntities.Authorization.TRUST_MARK_ISSUER.getValue(),
            TestFederationEntities.Authorization.TRUST_MARK_ISSUER.getValue() + "/certified",
            null, null, false));

    final String body = registryRecordSigner.signTrustMarkSubjects(tms).serialize();
    WireMock.stubFor(
        WireMock
            .get("/api/v1/federationservice/trustmarksubject_record?iss=%s&trustmark_id=%s"
                .formatted(TestFederationEntities.Authorization.TRUST_MARK_ISSUER.getValue(),
                    TestFederationEntities.Authorization.TRUST_MARK_ISSUER.getValue() + "/certified"
                ))
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(body)))
    );
  }

  private void mockModuleFor(final ModuleResponse moduleResponse, final String instanceId) throws JOSEException {
    final String body = registryRecordSigner.signModules(moduleResponse).serialize();
    WireMock.stubFor(
        WireMock
            .get("/api/v1/federationservice/submodules?instanceid=%s".formatted(URLEncoder.encode(instanceId,
                Charset.defaultCharset())))
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(body)))
    );
  }

  private static void createPolicy(final String policyBody, final String policyId) {
    WireMock
        .stubFor(WireMock.get("/api/v1/federationservice/policy_record?policy_id=%s".formatted(policyId))
            .willReturn(new ResponseDefinitionBuilder().withResponseBody(new Body(policyBody)))
        );
  }

  private void mockRecordsFor(final List<EntityRecord> build, final EntityID issuer) throws JOSEException {
    final String body = registryRecordSigner.signRecords(build).serialize();

    WireMock.stubFor(
        WireMock
            .get("/api/v1/federationservice/entity_record?iss=%s".formatted(issuer.getValue()))
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(body)))
    );
  }
}
