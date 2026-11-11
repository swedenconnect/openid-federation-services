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
package se.swedenconnect.oidf.common.entity.entity.integration.federation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests for {@link SubordinateListingRequest}.
 *
 * @author Martin Lindström
 */
class SubordinateListingRequestTest {

  @Test
  void falseValuesDoNotFilter() {
    Assertions.assertFalse(new SubordinateListingRequest(null, false, null, false).requiresFiltering());
    Assertions.assertFalse(new SubordinateListingRequest(List.of(), null, null, null).requiresFiltering());
  }

  @Test
  void trueValuesFilter() {
    Assertions.assertTrue(new SubordinateListingRequest(null, true, null, null).requiresFiltering());
    Assertions.assertTrue(new SubordinateListingRequest(null, null, null, true).requiresFiltering());
    Assertions.assertTrue(new SubordinateListingRequest(null, null, "https://tm.example.com", null)
        .requiresFiltering());
    Assertions.assertTrue(new SubordinateListingRequest(List.of("openid_provider"), null, null, null)
        .requiresFiltering());
  }
}
