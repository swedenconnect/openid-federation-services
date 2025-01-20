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
package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import se.digg.oidfed.common.entity.RecordVerificationException;
import se.digg.oidfed.common.entity.EntityRecordVerifier;
import se.digg.oidfed.common.validation.FederationAssert;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Verifier for {@link TrustMarkSubject}
 *
 * @author Per Fredrik Plars
 */
public class TrustMarkSubjectRecordVerifier extends EntityRecordVerifier {


  /**
   * Constructor.
   *
   * @param jwks to trust
   */
  public TrustMarkSubjectRecordVerifier(final JWKSet jwks) {
    super(jwks);
  }

  /**
   * @param jwtString containing trustmarksubject records
   * @return list of TrustMarkIssuerSubjects
   */
  public List<TrustMarkSubject> verifyTrustMarkSubjects(final String jwtString) {
    try {
      final List<Object> records = verify(jwtString)
          .getJWTClaimsSet()
          .getListClaim("trustmark_records");
      FederationAssert.assertNotEmpty(records,"Missing claim for:'trustmark_records' ");
      return records.stream()
          .map(o -> (Map<String,Object>)o)
          .map(TrustMarkSubject::fromJson)
          .toList();

    } catch (final ParseException | JOSEException e) {
      throw new RecordVerificationException("Failed to verify TrustMarkIssuerSubject record", e);
    }
  }

}
