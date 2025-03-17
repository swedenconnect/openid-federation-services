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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import se.digg.oidfed.common.entity.integration.registry.records.TrustMarkSubjectRecord;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.HostedRecord;
import se.digg.oidfed.common.entity.integration.registry.records.ModuleRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;
import se.digg.oidfed.common.entity.integration.registry.records.TrustMarkRecord;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

@Slf4j
public class RegistryMock {

  private final RegistryRecordSigner registryRecordSigner;
  public static final EntityID RP_FROM_REGISTRY_ENTITY = new EntityID("https://municipality.local.swedenconnect.se/rp-from-registry");
  @Getter
  private final int port;
  private WireMockServer wireMockServer;

  public RegistryMock(final int port) throws JOSEException, KeyStoreException, IOException, CertificateException,
      NoSuchAlgorithmException {
    final KeyStore keystore = KeyStore.getInstance(new ClassPathResource("signkey.p12").getFile(), "changeit".toCharArray());
    final JWKSet set = new JWKSet(JWK.load(
        keystore,
        "1",
        "changeit".toCharArray()
    ));
    this.port = port;
    this.wireMockServer = new WireMockServer(port);

    this.registryRecordSigner = new RegistryRecordSigner(new RSASSASigner(set.getKeys().getFirst().toRSAKey()));
  }
  
  public void stop() {
    this.wireMockServer.stop();
  }


  public void init(final String instanceId) throws Exception {
    this.wireMockServer.start();
    WireMock.configureFor(this.port);


    final String policyId = "my-super-policy";
    final List<EntityRecord> municipalityEntities = List.of(
        EntityRecord.builder()
            .issuer(TestFederationEntities.Municipality.TRUST_ANCHOR)
            .subject(RP_FROM_REGISTRY_ENTITY)
            .policyRecord(new PolicyRecord(policyId, Map.of()))
            .hostedRecord(HostedRecord.builder().metadata(Map.of("federation_entity", Map.of("organization_name",
                "Municipality"))).build())
            .build()
    );


    this.mockModuleFor(new ModuleRecord(), instanceId);
    this.mockRecordsFor(municipalityEntities, instanceId);

    final List<TrustMarkSubjectRecord> subjects = List.of(new TrustMarkSubjectRecord("https://subject.test", null, null, false));
    final List<TrustMarkRecord> tms = List.of(new TrustMarkRecord("https://trust-mark.test", "https://trust-mark" +
        ".test/cert", subjects, "https://logouri.test", null, null));

    final String body = this.registryRecordSigner.signTrustMarks(tms).serialize();
    final String endpoint = "/api/v1/federationservice/trustmarks_record?instanceid=%s".formatted(instanceId);
    log.info("{} will respond with {}", endpoint, body);
    WireMock.stubFor(
        WireMock
            .get(endpoint)
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(body)))
    );
  }

  private void mockModuleFor(final ModuleRecord moduleRecord, final String instanceId) throws JOSEException {
    final String body = registryRecordSigner.signModules(moduleRecord).serialize();
    final String endpoint = "/api/v1/federationservice/submodules?instanceid=%s".formatted(URLEncoder.encode(instanceId,
        Charset.defaultCharset()));
    log.info("{} will respond with {}", endpoint, body);
    WireMock.stubFor(
        WireMock
            .get(endpoint)
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(body)))
    );
  }

  private void mockRecordsFor(final List<EntityRecord> build, final String instanceId) throws JOSEException {
    final String body = registryRecordSigner.signRecords(build).serialize();
    final String endpoint = "/api/v1/federationservice/entity_record?instanceid=%s".formatted(instanceId);
    log.info("{} will respond with {}", endpoint, body);
    WireMock.stubFor(
        WireMock
            .get(endpoint)
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(body)))
    );
  }
}
