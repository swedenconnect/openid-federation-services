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

import java.util.Map;
import java.util.Objects;

/**
 * Wrapper class for providing metadata for a given request.
 * @param <T> request type
 *
 * @author Felix Hellman
 */
public final class FederationRequest<T>  {
  private final T parameters;
  private final Map<String, Object> federationEntityMetadata;

  /**
   * Constructor.
   * @param parameters for this request
   */
  public FederationRequest(final T parameters) {
    this(parameters, Map.of());
  }

  /**
   * Constructor.
   * @param parameters for this request
   * @param federationEntityMetadata for this request
   */
  public FederationRequest(final T parameters,
                           final Map<String, Object> federationEntityMetadata) {
    this.parameters = parameters;
    this.federationEntityMetadata = federationEntityMetadata;
  }

  /**
   * @return parameters of the request
   */
  public T parameters() {
    return this.parameters;
  }

  /**
   * @return metadata of the request
   */
  public Map<String, Object> federationEntityMetadata() {
    return this.federationEntityMetadata;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    final FederationRequest that = (FederationRequest) obj;
    return Objects.equals(this.parameters, that.parameters) &&
        Objects.equals(this.federationEntityMetadata, that.federationEntityMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.parameters, this.federationEntityMetadata);
  }

  @Override
  public String toString() {
    return "FederationRequest[" +
        "parameters=" + this.parameters.toString() + ", " +
        "federationEntityMetadata=" + this.federationEntityMetadata + ']';
  }

}
