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
package se.swedenconnect.oidf.service.entity;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Body;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.shaded.gson.ExclusionStrategy;
import com.nimbusds.jose.shaded.gson.FieldAttributes;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import se.swedenconnect.oidf.common.entity.entity.integration.DurationDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.InstantDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSKidReferenceLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSSerializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JsonRegistryLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.EntityRecordDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.JWKSerializer;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.keys.KeyProperty;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;
import se.swedenconnect.oidf.configuration.CompositeRecordSerializer;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
public class RegistryMock {

  private final RegistryRecordSigner registryRecordSigner;
  public static final EntityID RP_FROM_REGISTRY_ENTITY = new EntityID("https://municipality.local.swedenconnect.se/rp-from-registry");
  @Getter
  private final int port;
  private WireMockServer wireMockServer;

  public RegistryMock(final int port) throws JOSEException, KeyStoreException, IOException,
      CertificateException,
      NoSuchAlgorithmException, ParseException {
    final KeyStore keystore = KeyStore.getInstance(new ClassPathResource("signkey.p12").getFile(), "changeit".toCharArray());
    final Map<String, Object> jsonObject = JWK.load(
        keystore,
        "1",
        "changeit".toCharArray()
    ).toJSONObject();
    jsonObject.put("kid", "359433581122628090150675142465804663870388233428");
    final JWK load = JWK.parse(jsonObject);
    final JWKSet set = new JWKSet(load);
    this.port = port;
    this.wireMockServer = new WireMockServer(port);

    final KeyRegistry registry = new KeyRegistry();
    final KeyProperty property = new KeyProperty();
    property.setAlias("sign-key-1");
    property.setKey(load);
    property.setMapping("federation");
    registry.register(property);
    final JWKSKidReferenceLoader jwksKidReferenceLoader = new JWKSKidReferenceLoader(registry);
    final JsonRegistryLoader jsonRegistryLoader = new JsonRegistryLoader(this.createGson(jwksKidReferenceLoader, registry));
    this.registryRecordSigner = new RegistryRecordSigner(
        new RSASSASigner(set.getKeys().getFirst().toRSAKey()),
        jsonRegistryLoader
    );
  }

  public void stop() {
    this.wireMockServer.stop();
  }


  public void init(final String instanceId) throws Exception {
    this.wireMockServer.start();
    WireMock.configureFor(this.port);

    final RSAKey key = new RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .generate();
    final List<EntityRecord> municipalityEntities = List.of(
        EntityRecord.builder()
            .entityIdentifier(new EntityID(TestFederationEntities.IM.INTERMEDIATE.getValue() + "/dynamic"))
            .jwks(new JWKSet(key))
            .build()
    );


    this.mockModuleFor(new ModuleRecord(), instanceId);
    this.mockRecordsFor(municipalityEntities, instanceId);
  }

  public void initCustom(final String instanceId) throws Exception {
    this.wireMockServer.start();
    WireMock.configureFor(this.port);
    final String moduleJson = new ClassPathResource("modules.json").getContentAsString(StandardCharsets.UTF_8);
    final String entityJson = new ClassPathResource("testentities.json").getContentAsString(StandardCharsets.UTF_8);

    final SignedJWT moduleJwt = this.registryRecordSigner.signJson("module_records", moduleJson, "module-trustMarkSubjects" +
                                                                                                 "+jwt");
    final String entityJwt =
        this.registryRecordSigner.signJson("entity_records", entityJson, "entity-trustMarkSubjects+jwt").serialize();


    final String endpoint = "/api/v1/federationservice/submodules?instanceid=%s".formatted(URLEncoder.encode(instanceId,
        Charset.defaultCharset()));
    log.info("{} will respond with {}", endpoint, moduleJwt.serialize());
    WireMock.stubFor(
        WireMock
            .get(endpoint)
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(moduleJwt.serialize())))
    );
    final String endpointEntity = "/api/v1/federationservice/entity_record?instanceid=%s".formatted(instanceId);
    log.info("{} will respond with {}", endpoint, entityJwt);
    WireMock.stubFor(
        WireMock
            .get(endpointEntity)
            .willReturn(new ResponseDefinitionBuilder().withStatus(200).withResponseBody(new Body(entityJwt)))
    );
    System.out.println("Started mock");
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

  private Gson createGson (final JWKSKidReferenceLoader loader, final KeyRegistry registry) {
    return new GsonBuilder()
        .addDeserializationExclusionStrategy(new ExclusionStrategy() {
          @Override
          public boolean shouldSkipField(final FieldAttributes fieldAttributes) {
            return false;
          }

          @Override
          public boolean shouldSkipClass(final Class<?> aClass) {
            return false;
          }
        })
        .registerTypeAdapter(Duration.class, new DurationDeserializer())
        .registerTypeAdapter(Instant.class, new InstantDeserializer())
        .registerTypeAdapter(EntityID.class, new EntityIdentifierDeserializer())
        .registerTypeAdapter(TrustMarkType.class, new TrustMarkIdentifierDeserializer())
        .registerTypeAdapter(JWKSet.class, new JWKSSerializer(loader, loader))
        .registerTypeAdapter(CompositeRecord.class, new CompositeRecordSerializer())
        .registerTypeAdapter(EntityRecord.class, new EntityRecordDeserializer(loader, registry))
        .create();
  }
}
