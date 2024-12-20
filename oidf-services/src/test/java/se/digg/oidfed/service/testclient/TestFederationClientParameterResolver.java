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
package se.digg.oidfed.service.testclient;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.service.rest.RestClientFactory;
import se.digg.oidfed.service.rest.RestClientProperties;

public class TestFederationClientParameterResolver implements ParameterResolver {

  @Override
  public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(FederationClients.class);
  }

  @Override
  public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
    final SslBundles bundles = SpringExtension.getApplicationContext(extensionContext).getBean(SslBundles.class);
    final RestClientProperties.RestClientProperty property = new RestClientProperties.RestClientProperty();
    property.setTrustStoreBundleName("oidf-internal");
    property.setName("test");
    final RestClient client = new RestClientFactory(bundles).create(property);
    return new FederationClients(client);
  }
}
