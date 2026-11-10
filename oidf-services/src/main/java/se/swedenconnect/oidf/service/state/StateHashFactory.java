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
package se.swedenconnect.oidf.service.state;

import com.nimbusds.jose.shaded.gson.Gson;
import lombok.AllArgsConstructor;
import org.bouncycastle.util.encoders.Hex;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Class responsible for hashing internal state.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class StateHashFactory {
  private final Gson gson;
  /**
   * @param record to hash
   * @return hash of record
   * @throws Exception
   */
  public String hashState(final CompositeRecord record) throws Exception {
    final String json = this.gson.toJson(record);
    final MessageDigest digest = MessageDigest.getInstance("SHA-256");
    final byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
    return new String(Hex.encode(hash));
  }
}
