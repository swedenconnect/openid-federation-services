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
package se.swedenconnect.oidf.common.entity.entity.integration;

import java.io.Serializable;

/**
 * Serializable response holder for use with {@link ModuleResponseCache}.
 * Carries both the response body and the content type so caches can replay
 * responses without losing HTTP semantics.
 *
 * @param body        the serialized response body
 * @param contentType the HTTP content-type of the response
 * @param statusCode  the HTTP status code of the response
 * @author Felix Hellman
 */
public record CachedResponse(String body, String contentType, int statusCode) implements Serializable {
}
