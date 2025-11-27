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
package se.swedenconnect.oidf.service.keys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KeyEntryTest {
  @Test
  void testParseKey() {
    final String key = """
        ewogICAgICAgICJhbGciOiAiRVM1MTIiLAogICAgICAgICJjcnYiOiAiUC01MjEiLAogICAgICAgICJraWQiOiAiQjRkbGU1ajJYT19yLXZsdG9qSDB6X0FHTEhUSjVTamR0di04MDA1YzRMNCIsCiAgICAgICAgImt0eSI6ICJFQyIsCiAgICAgICAgInVzZSI6ICJzaWciLAogICAgICAgICJ4IjogIkFJMXRINm5qRlRBT1hiVDFQVkp3QS1VaWh1R3dwdk5HX1BYWm50R1lIM0o4QzFDcjd2MmZiVkxyM1l4VnR3bW10cGZsZWVoN3dxUWtndWdRWm1iVjV6T3kiLAogICAgICAgICJ5IjogIkFFdTNoa1NycTVHZVN5ZW5rUkxfR180QkJlVXRpLXV5ZDVOQzBiZmlkYlR2VnBkdXZVTHVHSGV2QXRZUUFmUnJYYTlOekFkTHVJQkFpbWFXbWNlLTBmc2wiCiAgICAgIH0K
        """;

    final KeyEntry parsedKey = new KeyEntry("name", key);

    Assertions.assertEquals("B4dle5j2XO_r-vltojH0z_AGLHTJ5Sjdtv-8005c4L4", parsedKey.getKey().getKeyID());
  }

}