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
package se.digg.oidfed.service.trustmarkissuer;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import se.digg.oidfed.service.IntegrationTestParent;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@ActiveProfiles({ "trustmark", "integration-test" })
class TrustMarkIT extends IntegrationTestParent {


  @BeforeEach
  public void setup() {
    RestAssured.port = this.serverPort;
    RestAssured.basePath = "/tm";
  }

  @Test
  public void testTrustMark() {
    given()
        .param("trust_mark_id", "http://tm.digg.se/sdk")
        .param("sub", "http://www.pensionsmyndigheten.se/openidfed")
        .when()
        .get("/trust_mark")
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType("application/trust-mark+jwt")
        .log().all();
  }

  @Test
  public void testTrustMarkListing() {
    given()
        .param("trust_mark_id", "http://tm.digg.se/sdk")
        .param("sub", "http://www.pensionsmyndigheten.se/openidfed")
        .when()
        .get("/trust_mark_listing")
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType(ContentType.JSON)
        .body("$", hasSize(greaterThan(0)));
  }

  @Test
  public void testTrustMarkStatusActive() {
    given()
        .param("trust_mark_id", "http://tm.digg.se/sdk")
        .param("sub", "http://www.pensionsmyndigheten.se/openidfed")
        .when()
        .post("/trust_mark_status")
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType(ContentType.JSON)
        .body("active", is(true))
        .log().all();
  }

  @Test
  public void testTrustMarkStatusNotActive() {
    given()
        .param("trust_mark_id", "http://tm.digg.se/sdk")
        .param("sub", "http://www.pensionsmyndigheten.se/notfound")
        .when()
        .post("/trust_mark_status")
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType(ContentType.JSON)
        .body("active", is(false))
        .log().all();
  }


  @Test
  public void testTrustMarkStatusError() {
    given()
        .param("trust_mark_id", "http://tm.digg.se/notfound")
        .when()
        .post("/trust_mark_status")
        .then().log().all()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .contentType(ContentType.JSON)
        .body("error", is("not_found"))
        .body("error_description", is("TrustMark can not be found for trust_mark_id:'http://tm.digg.se/notfound'"));

  }

}
