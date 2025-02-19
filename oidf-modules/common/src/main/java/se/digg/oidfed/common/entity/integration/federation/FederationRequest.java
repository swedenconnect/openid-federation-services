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
package se.digg.oidfed.common.entity.integration.federation;

import java.io.Serial;
import java.io.Serializable;
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
  private final Map<String, String> federationEntityMetadata;
  private final Boolean useCachedValue;

  /**
   * Constructor.
   * @param parameters for this reuqest
   * @param federationEntityMetadata for this request
   * @param useCachedValue if false, a request will skip cache to fetch from integration
   */
  public FederationRequest(final T parameters,
                           final Map<String, String> federationEntityMetadata,
                           final Boolean useCachedValue) {
    this.parameters = parameters;
    this.federationEntityMetadata = federationEntityMetadata;
    this.useCachedValue = useCachedValue;
  }

  /**
   * @return parameters of the reuqest
   */
  public T parameters() {
    return this.parameters;
  }

  /**
   * @return metadata of the request
   */
  public Map<String, String> federationEntityMetadata() {
    return this.federationEntityMetadata;
  }

  /**
   * @return if false, a request will skip cache to fetch from integration
   */
  public Boolean useCachedValue() {
    return this.useCachedValue;
  }

  @Override
  public boolean equals(final Object obj) {
    //Do not differentiate objects depending on flag value useCachedValue
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    final FederationRequest that = (FederationRequest) obj;
    return Objects.equals(this.parameters, that.parameters) &&
        Objects.equals(this.federationEntityMetadata, that.federationEntityMetadata);
  }

  @Override
  public int hashCode() {
    //Do not differentiate objects depending on flag value useCachedValue
    return Objects.hash(this.parameters, this.federationEntityMetadata);
  }

  @Override
  public String toString() {
    return "FederationRequest[" +
        "parameters=" + this.parameters.toString() + ", " +
        "federationEntityMetadata=" + this.federationEntityMetadata + ", " +
        "useCachedValue=" + this.useCachedValue + ']';
  }

}
