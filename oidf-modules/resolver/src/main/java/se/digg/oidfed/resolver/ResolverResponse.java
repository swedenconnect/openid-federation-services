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
package se.digg.oidfed.resolver;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import lombok.Builder;
import net.minidev.json.JSONObject;

import java.util.List;

/**
 *
 * @param entityStatement of the resolved entity
 * @param metadata that has been processed by the policy
 * @param trustMarkEntries for the trust chain
 * @param trustChain for this response
 *
 * @author Felix Hellman
 */
@Builder
public record ResolverResponse(EntityStatement entityStatement, JSONObject metadata,
    List<TrustMarkEntry> trustMarkEntries, List<EntityStatement> trustChain) {
}
