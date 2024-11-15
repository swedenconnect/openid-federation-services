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

package se.digg.oidfed.trustmarkissuer;

import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * InMemory Implementation of
 *
 * @author Per Fredrik Plars
 */
public class TrustMarkIssuerSubjectInMemLoader implements TrustMarkIssuerSubjectLoader{

  private final List<TrustMarkIssuerSubject> trustMarkPropertiesList;

  /**
   * Default constructor
   */
  public TrustMarkIssuerSubjectInMemLoader() {
    this.trustMarkPropertiesList = new ArrayList<>();
  }

  /**
   * Takes a list of TrustMarkIssuerSubject
   * @param trustMarkPropertiesList List vith TrustMarkIssuerSubject
   */
  public TrustMarkIssuerSubjectInMemLoader(List<TrustMarkIssuerSubject> trustMarkPropertiesList) {
    this.trustMarkPropertiesList = trustMarkPropertiesList;
    this.trustMarkPropertiesList.forEach(TrustMarkIssuerSubject::validate);
  }



  @Override
  public List<TrustMarkIssuerSubject> loadSubject(final String issuerEntityId, final TrustMarkId trustMarkId,
      final Optional<String> subject) {

    return trustMarkPropertiesList.stream()
        .filter(trustMarkIssuerSubject -> Objects.isNull(subject) || subject.isEmpty() ||
            subject.filter(sub -> trustMarkIssuerSubject.sub().equals(sub)).isPresent())
        .toList();

  }

  /**
   * Register new TrustMarkIssuerSubject
   * @param trustMarkIssuerSubject TrustMarkIssuerSubject to be registered
   */
  public void register(TrustMarkIssuerSubject trustMarkIssuerSubject){
    trustMarkIssuerSubject.validate();
    trustMarkPropertiesList.add(trustMarkIssuerSubject);
  }



}
