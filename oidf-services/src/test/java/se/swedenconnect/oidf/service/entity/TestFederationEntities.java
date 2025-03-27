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
package se.swedenconnect.oidf.service.entity;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;

public class TestFederationEntities {
  public static class Anarchy {
    public static EntityID TRUST_ANCHOR = new EntityID("http://localhost:11111/anarchy/ta");
    public static EntityID RESOLVER = new EntityID("http://localhost:11111/anarchy/resolver");
  }
  public static class Policy {
    public static EntityID TRUST_ANCHOR = new EntityID("http://localhost:11111/policy/ta");
    public static EntityID RESOLVER = new EntityID("http://localhost:11111/policy/resolver");
  }
  public static class TrustMarkIssuer {
    public static EntityID TRUST_ANCHOR = new EntityID("http://localhost:11111/trust_mark_issuer/ta");
    public static EntityID RESOLVER = new EntityID("http://localhost:11111/trust_mark_issuer/resolver");
  }
  public static class EntityType {
    public static EntityID TRUST_ANCHOR = new EntityID("http://localhost:11111/entity_type/ta");
    public static EntityID RESOLVER = new EntityID("http://localhost:11111/entity_type/resolver");
  }
  public static class Path {
    public static EntityID TRUST_ANCHOR = new EntityID("http://localhost:11111/path/ta");
    public static EntityID RESOLVER = new EntityID("http://localhost:11111/path/resolver");
  }
  public static class Naming {
    public static EntityID TRUST_ANCHOR = new EntityID("http://localhost:11111/naming/ta");
    public static EntityID RESOLVER = new EntityID("http://localhost:11111/naming/resolver");
  }

  public static class IM {
    public static EntityID INTERMEDIATE = new EntityID("http://localhost:11111/im");
    public static EntityID TRUST_MARK_ISSUER = new EntityID("http://localhost:11111/im/tmi");
    public static EntityID OP = new EntityID("http://localhost:11111/im/op");

    public static class NestedIM {
      public static EntityID INTERMEDIATE = new EntityID("http://localhost:11111/im/im");
      public static EntityID OP = new EntityID("http://localhost:11111/im/im/op");
    }
  }
}
