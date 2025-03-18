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
package se.swedenconnect.oidf.common.entity.entity.integration.properties;

import com.nimbusds.jose.jwk.JWK;

import java.time.Duration;
import java.util.List;

/**
 * Properties for the resolver.
 *
 * @param trustAnchor The trust anchor used by this resolve entity
 * @param resolveResponseDuration The validity duration of issued resolve responses
 * @param trustedKeys Keys trusted by this resolver to validate Entity Statement chains
 * @param entityIdentifier for the resolver
 * @param stepRetryTime time to wait before retrying a step that has failed
 * @author Felix Hellman
 */
public record ResolverProperties(
    String trustAnchor,
    Duration resolveResponseDuration,
    List<JWK> trustedKeys,
    String entityIdentifier,
    Duration stepRetryTime) {
}

