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

package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkSubjectRecord;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Testing conversion TrustMarkIssuerSubject
 *
 * @author Per Fredrik Plars
 */
@Slf4j
class TrustMarkSubjectRecordTest {

  @Test
  public void testFromJson() throws ParseException {

    final String rawJwk ="eyJraWQiOiJVblN1REpEdW1ZbEQxSE43SGNZY3czamRXZUNfZGNQRkhXRVdRVzlEUm13IiwidHlwIjoidHJ1c3RtYXJrLXJlY29yZHMrand0IiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJodHRwOi8vb2lkZi1yZWdpc3RyeS5zd2VkZW5jb25uZWN0LnNlL3Rlc3QiLCJpYXQiOjE3MzM5ODk5MTksImp0aSI6ImIwNzVjMzI2LTU0YjMtNDM5OC04M2E1LTYwOTZmY2QxMDNmZSIsInRydXN0bWFya19yZWNvcmRzIjpbeyJ0cnVzdF9tYXJrX3N1YmplY3RfcmVjb3JkX2lkIjoiZThmZWIyNjEtZWYzNi00ZjU3LWJlNzUtYjYyMjZhODUxMTBmIiwiaXNzdWVyIjoiaHR0cDovL3d3dy5zd2VkZW5jb25uZWN0LnNlL2lzc3VlciIsInRydXN0X21hcmtfaWQiOiJodHRwOi8vd3d3LnN3ZWRlbmNvbm5lY3Quc2UvdHJ1c3RtYXJraWQiLCJzdWJqZWN0IjoiaHR0cDovL3d3dy5zd2VkZW5jb25uZWN0LnNlL3N1YmplY3QiLCJyZXZva2VkIjp0cnVlLCJncmFudGVkIjoiMjAyNC0xMi0xMlQwNzo1MTo1My4xNDk1MTY1NzJaIiwiZXhwaXJlcyI6IjIwMjQtMTItMjJUMDc6NTE6NTMuMTQ5NTUwODc0WiJ9LHsidHJ1c3RfbWFya19zdWJqZWN0X3JlY29yZF9pZCI6ImI1OWQ1MjAzLThkYjEtNDM3OS05NDA5LTJmZjY0MWU4MjA0MiIsImlzc3VlciI6Imh0dHA6Ly93d3cuc3dlZGVuY29ubmVjdC5zZS9pc3N1ZXIiLCJ0cnVzdF9tYXJrX2lkIjoiaHR0cDovL3d3dy5zd2VkZW5jb25uZWN0LnNlL3RydXN0bWFya2lkIiwic3ViamVjdCI6Imh0dHA6Ly93d3cuc3dlZGVuY29ubmVjdC5zZS9zdWJqZWN0MiIsInJldm9rZWQiOnRydWUsImdyYW50ZWQiOm51bGwsImV4cGlyZXMiOm51bGx9XX0.oY8Qfq0bFkICmC2xTZPS535VrsCY6enFWDN96-jOdsmEwQjHysg79eo0FreTVb3hgYrOSOPUFKM2P89rBWIzoCq-PZrBiDDE-jil2kEFuRjNFr54EWUbrciB4YHy_-6tJUgobmVfpvxvrsVDA0QTr49J3MrVwc5g6S9S-OIMuzz-tBNfLzveSmdX8z_Jsan_A_LbPkMnOxN2rFBtnCjxRyTsxOrJT55TBDM4wk_tymZbuZN6zasix15u4P62H4yWdQmlC4hAX7NVb-RX0hVWxgQU1A3vZxye6qr0-1ge3nDdWIvKyxsWg7gYC94tKpK3x4eFsj9OuJbx4sdob9rnKB63reotK1ZQKO_vifJ94QhIiSo9LNtzHnhq-DNnfGedct2cCiHViHA3SpPykCpyzlZUAjo39Nl8vtBrtkTSomCLN3neN3dEG8E6Dja_cCXUgptqHEC8RSfwDCp8O5545aU4bGvQq3R2Cju8Mfqui2MZ2qloNKndyNAeI2gQGBA3";

    final SignedJWT tms = SignedJWT.parse(rawJwk);
    final JWTClaimsSet claimsSet = tms.getJWTClaimsSet();
    final List<Object> records = claimsSet.getListClaim("trustmark_records");

    records.stream()
        .map(o -> (Map<String,Object>)o)
        .map(TrustMarkSubjectRecord::fromJson)
        .forEach(System.out::println);
  }

}