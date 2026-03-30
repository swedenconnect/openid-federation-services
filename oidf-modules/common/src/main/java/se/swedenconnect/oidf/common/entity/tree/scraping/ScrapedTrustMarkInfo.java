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
package se.swedenconnect.oidf.common.entity.tree.scraping;

import com.nimbusds.jwt.SignedJWT;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;

/**
 * Holds scraped trust mark information for a specific issuer, type, and subject combination.
 *
 * @param trustMarkIssuer        entity ID of the trust mark issuer
 * @param trustMarkType          the trust mark type identifier
 * @param trustMarkSubject       entity ID of the trust mark subject
 * @param trustMark              the signed trust mark JWT
 * @param trustMarkStatusResponse the status response for the trust mark
 * @author Felix Hellman
 */
public record ScrapedTrustMarkInfo(String trustMarkIssuer,
                                   String trustMarkType,
                                   String trustMarkSubject,
                                   SignedJWT trustMark,
                                   TrustMarkStatusResponse trustMarkStatusResponse) {
}
