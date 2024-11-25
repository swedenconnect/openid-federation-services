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
 *  limitations under the License.
 */
package se.digg.oidfed.service;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@ActiveProfiles({"integration-test" })
@Disabled
class GeneralErrorHandlingIT extends IntegrationTestParent {


  @BeforeEach
  public void setup() {
    RestAssured.port = this.serverPort;
    RestAssured.basePath = "/";
  }

  @Test
  public void testBrowserRequestExpectNotFound() {
    given()
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,"
            + "image/apng,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Connection", "keep-alive")
        .when().log().all()
        .contentType(ContentType.HTML)
        .get("/notfound/myfrend")
        .then().log().all()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .body("error", Matchers.equalTo("not_found"))
        .body("error_description", Matchers.equalTo("No static resource notfound/myfrend."))
        .contentType(ContentType.JSON);
  }


  @Test
  public void testMethodNotSupported() {
    given()
        .when().log().all()
        .contentType(ContentType.JSON)
        .delete("/tm/trust_mark")
        .then().log().all()
        .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value())
        .body("error", Matchers.equalTo("method_not_allowed"))
        .body("error_description", Matchers.equalTo("Method 'DELETE' is not supported."))
        .contentType(ContentType.JSON);
  }

  @Test
  public void testMissingParamExpect400() {
    given()
        .when().log().all()
        .contentType("VerySpecialContentType")
        .get("/tm/trust_mark_listing")
        .then().log().all()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .body("error", Matchers.equalTo("bad_request"))
        .body("error_description", Matchers.equalTo("Required parameter 'trust_mark_id' is not present."))
        .contentType(ContentType.JSON);
  }





}
