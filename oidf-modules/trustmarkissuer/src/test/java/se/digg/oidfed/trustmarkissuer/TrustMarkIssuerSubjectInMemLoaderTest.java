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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * openid-federation-services
 *
 * @author Per Fredrik Plars
 */
@Slf4j
class TrustMarkIssuerSubjectInMemLoaderTest {

  @Test
  void loadSubject() {
    final TrustMarkIssuerSubjectInMemLoader loader = new TrustMarkIssuerSubjectInMemLoader();
    loader.register(TrustMarkIssuerSubject.builder().sub("http://sub1.digg.se")
        .expires(Optional.empty()).granted(Optional.empty())
        .build());
    loader.register(TrustMarkIssuerSubject.builder().sub("http://sub2.digg.se")
        .expires(Optional.empty()).granted(Optional.empty())
        .build());

    assertEquals(2,loader.loadSubject(null,null, null).size());
    assertEquals(2,loader.loadSubject(null,null, Optional.empty()).size());
    assertEquals(1,loader.loadSubject(null,null,
        Optional.of("http://sub2.digg.se")).size());
    assertEquals(0,loader.loadSubject(null,null,
        Optional.of("http://nofound.digg.se")).size());

  }
}