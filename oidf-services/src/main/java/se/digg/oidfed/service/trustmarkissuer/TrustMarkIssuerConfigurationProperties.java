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
package se.digg.oidfed.service.trustmarkissuer;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import se.digg.oidfed.trustmarkissuer.configuration.TrustMarkIssuerProperties;
import se.digg.oidfed.trustmarkissuer.configuration.TrustMarkProperties;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.time.Duration;
import java.util.List;

/**
 * Properties for trust mark issuer.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@ConfigurationProperties(TrustMarkIssuerConfigurationProperties.PROPERTY_PATH)
public class TrustMarkIssuerConfigurationProperties extends TrustMarkProperties {
  public static final String PROPERTY_PATH = "openid.federation.trust-mark-issuer";

  /** Set to true if this module should be active or not. */
  private Boolean active;





}
