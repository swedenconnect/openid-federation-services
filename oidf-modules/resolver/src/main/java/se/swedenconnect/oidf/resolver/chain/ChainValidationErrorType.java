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
package se.swedenconnect.oidf.resolver.chain;

/**
 * Stable machine-readable codes for chain validation failures.
 *
 * @author Felix Hellman
 */
public enum ChainValidationErrorType {
  LEAF_SIGNATURE_INVALID,
  LEAF_WRONG_JWK_KID,
  LEAF_EXPIRED,
  LEAF_NO_ISSUE_TIME,
  LEAF_ISSUE_TIME_IN_FUTURE,
  LEAF_NO_EXPIRATION_TIME,
  TRUST_ANCHOR_INVALID,
  CHAIN_LINK_SIGNATURE_INVALID,
  STATEMENT_EXPIRED,
  CHAIN_TOO_SHORT,
  ENTITY_TYPE_CONSTRAINT_VIOLATION,
  MAX_PATH_LENGTH_EXCEEDED,
  NAMING_CONSTRAINT_EXCLUDED_VIOLATION,
  NAMING_CONSTRAINT_PERMITTED_VIOLATION,
  UNSUPPORTED_CRITICAL_CLAIMS,
}
