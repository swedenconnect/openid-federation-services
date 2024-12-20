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
package se.digg.oidfed.service.entity;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;

public class TestFederationEntities {
  public static class Authorization {
    public static final EntityID TRUST_ANCHOR = new EntityID("https://authorization.local.swedenconnect" +
        ".se/authorization-ta");
    public static final EntityID RESOLVER = new EntityID("https://authorization.local.swedenconnect" +
        ".se/authorization-resolver");
    public static final EntityID TRUST_MARK_ISSUER = new EntityID("https://authorization.local.swedenconnect" +
        ".se/authorization-tmi");
    public static final EntityID OP_1 = new EntityID("https://authorization.local.swedenconnect" +
        ".se/op-1");
    public static final EntityID OP_2 = new EntityID("https://authorization.local.swedenconnect" +
        ".se/op-2");
  }

  public static class Municipality {
    public static final EntityID RESOLVER =
        new EntityID("https://municipality.local.swedenconnect.se/municipality-resolver");
    public static final EntityID TRUST_ANCHOR =
        new EntityID("https://municipality.local.swedenconnect.se/municipality-ta");
    public static final EntityID TRUST_MARK_ISSUER =
        new EntityID("https://municipality.local.swedenconnect.se/municipality-tmi");
  }

  public static class PrivateSector {
    public static final EntityID RESOLVER = new EntityID("https://private.local.swedenconnect.se/private-resolver");
    public static final EntityID TRUST_ANCHOR =    new EntityID("https://private.local.swedenconnect.se/private-ta");
    public static final EntityID TRUST_MARK_ISSUER =    new EntityID("https://private.local.swedenconnect" +
        ".se/private-tmi");
  }
}
