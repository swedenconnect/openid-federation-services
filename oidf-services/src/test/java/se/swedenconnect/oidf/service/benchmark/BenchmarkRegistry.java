/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf.service.benchmark;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fake registry serving signed entity and module records over the same wire contract as the real registry.
 *
 * <p>This is the only place that knows the registry contract. If the contract changes (paging, deltas, ...)
 * this class is what needs to follow.</p>
 *
 * @author Felix Hellman
 */
public class BenchmarkRegistry implements AutoCloseable {

  private static final String BASE_PATH = "/api/v1/federationservice";

  private final HttpServer server;
  private final RSASSASigner signer;
  private final String kid;
  private final Map<String, Object> publicJwk;
  private final AtomicReference<byte[]> entityRecords = new AtomicReference<>(new byte[0]);
  private final AtomicReference<byte[]> moduleRecords = new AtomicReference<>(new byte[0]);
  private final AtomicLong entityRequests = new AtomicLong();
  private final AtomicLong moduleRequests = new AtomicLong();
  private final AtomicLong bytesServed = new AtomicLong();

  /**
   * Payload sizes of one published state.
   *
   * @param entityJsonBytes size of the entity_records json
   * @param moduleJsonBytes size of the module_records json
   * @param entityWireBytes size of the signed entity response
   * @param moduleWireBytes size of the signed module response
   * @param buildMillis     time to generate and sign both responses
   */
  public record Payload(long entityJsonBytes, long moduleJsonBytes, long entityWireBytes, long moduleWireBytes,
                        long buildMillis) {
  }

  /**
   * Creates and starts the registry on a random port, signing with the test registry key (signkey.p12).
   *
   * @throws Exception if the key can not be loaded or the server can not start
   */
  public BenchmarkRegistry() throws Exception {
    final KeyStore keyStore = KeyStore.getInstance(
        new ClassPathResource("signkey.p12").getFile(), "changeit".toCharArray());
    final X509Certificate certificate = (X509Certificate) keyStore.getCertificate("1");
    this.kid = certificate.getSerialNumber().toString();
    final RSAKey key = new RSAKey.Builder(JWK.load(keyStore, "1", "changeit".toCharArray()).toRSAKey())
        .keyID(this.kid)
        .x509CertChain(null)
        .build();
    this.signer = new RSASSASigner(key);
    this.publicJwk = key.toPublicJWK().toJSONObject();

    this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    this.server.createContext(BASE_PATH + "/entity_record", exchange -> {
      this.entityRequests.incrementAndGet();
      this.respond(exchange, this.entityRecords.get());
    });
    this.server.createContext(BASE_PATH + "/submodules", exchange -> {
      this.moduleRequests.incrementAndGet();
      this.respond(exchange, this.moduleRecords.get());
    });
    this.server.start();
  }

  /**
   * @return base uri to configure as federation.registry.integration.client.base-uri
   */
  public String baseUri() {
    return "http://localhost:%d%s".formatted(this.server.getAddress().getPort(), BASE_PATH);
  }

  /**
   * @return public jwk of the registry/federation key
   */
  public Map<String, Object> publicJwk() {
    return new HashMap<>(this.publicJwk);
  }

  /**
   * @return key reference for hosted entities
   */
  public String hostedKeyReference() {
    return "federation:" + this.kid;
  }

  /**
   * Generates and publishes the current state of the federation.
   *
   * @param federation to publish
   * @return payload sizes
   * @throws JOSEException if signing fails
   */
  public Payload publish(final SyntheticFederation federation) throws JOSEException {
    final long start = System.nanoTime();
    final String entityJson = federation.entityRecordsJson();
    final String moduleJson = federation.moduleRecordsJson();
    final byte[] entityBody = this.sign("entity_records", entityJson, "entity-trustMarkSubjects+jwt");
    final byte[] moduleBody = this.sign("module_records", moduleJson, "module-trustMarkSubjects+jwt");
    this.entityRecords.set(entityBody);
    this.moduleRecords.set(moduleBody);
    return new Payload(
        entityJson.getBytes(StandardCharsets.UTF_8).length,
        moduleJson.getBytes(StandardCharsets.UTF_8).length,
        entityBody.length,
        moduleBody.length,
        (System.nanoTime() - start) / 1_000_000);
  }

  /**
   * @return a signed notification to post to the service's /registry/notify endpoint
   * @throws JOSEException if signing fails
   */
  public String notification() throws JOSEException {
    final SignedJWT jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(this.kid).build(),
        new JWTClaimsSet.Builder().issueTime(new Date()).claim("notification", "changed").build());
    jwt.sign(this.signer);
    return jwt.serialize();
  }

  /**
   * @return number of entity record fetches served
   */
  public long entityRequests() {
    return this.entityRequests.get();
  }

  /**
   * @return number of module record fetches served
   */
  public long moduleRequests() {
    return this.moduleRequests.get();
  }

  /**
   * @return total response bytes served
   */
  public long bytesServed() {
    return this.bytesServed.get();
  }

  @Override
  public void close() {
    this.server.stop(0);
  }

  private byte[] sign(final String claim, final String json, final String type) throws JOSEException {
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .claim(claim, json)
        .expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .issueTime(new Date())
        .build();
    final SignedJWT jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(this.kid).type(new JOSEObjectType(type)).build(),
        claims);
    jwt.sign(this.signer);
    return jwt.serialize().getBytes(StandardCharsets.US_ASCII);
  }

  private void respond(final HttpExchange exchange, final byte[] body) throws IOException {
    exchange.getResponseHeaders().add("Content-Type", "application/jwt");
    exchange.sendResponseHeaders(200, body.length);
    try (final OutputStream out = exchange.getResponseBody()) {
      out.write(body);
    }
    this.bytesServed.addAndGet(body.length);
  }
}
