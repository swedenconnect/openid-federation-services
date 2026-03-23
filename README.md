# OpenID Federation Services 

The OpenID Federation Services implements necessary apis to support OpenID Federation in accordance
to https://openid.net/specs/openid-federation-1_0.html

This service component can work standalone with management of properties or in managed mode
using a [registry](https://github.com/swedenconnect/openid-federation-registry) sevice to enable management using a web
interface.

## Use cases
## Use Case Overview

OpenID Federation Services exposes APIs for federation metadata resolution, trust anchor and intermediate federation data, trust mark issuance and validation, and service configuration and monitoring.

The overview below summarizes the main API use cases and the supporting capabilities of the service.

For detailed use case descriptions, see [Use Cases](./docs/usecases/use-cases.md).

```mermaid
flowchart LR

  %% Actors
  OIDFC[OpenID Federation Client]
  SvcOp[Service Operator]
  Registry[OpenID Federation Registry]

  %% System boundary
  subgraph OFS["OpenID Federation Services"]

    subgraph Resolver["Resolver API"]
      R1([Resolve and verify entity metadata])
    end

    subgraph TA["Trust Anchor API"]
      TA1([Discover entities])
      TA2([Get subordinate listing])
      TA3([Get subordinate statement])
    end

    subgraph INT["Intermediate API"]
      I1([Get subordinate listing])
      I2([Get subordinate statement])
    end

    subgraph TMI["Trust Mark Issuer API"]
      TM1([Get trust mark])
      TM2([Verify trust mark status])
      TM3([List trust marks])
    end

    subgraph CFG["Configuration and status"]
      C1([Module configuration])
      C2([Monitoring])
      C3([Registry-based configuration])
    end

    subgraph CORE["Shared supporting capabilities"]
      S1([Fetch external federation metadata])
      S2([Verify signatures])
      S3([Cache federation metadata])
    end
  end

  %% Actor relations
  OIDFC --> R1
  OIDFC --> TA1
  OIDFC --> TA2
  OIDFC --> TA3
  OIDFC --> I1
  OIDFC --> I2
  OIDFC --> TM1
  OIDFC --> TM2
  OIDFC --> TM3

  SvcOp --> C1
  SvcOp --> C2
  SvcOp --> C3

  Registry --> C3

  %% Dependencies
  R1 -.-> S1
  R1 -.-> S2
  R1 -.-> S3

  TA1 -.-> S1
  TA1 -.-> S2
  TA1 -.-> S3

  TM1 -.-> S2
  TM2 -.-> S2
  TM3 -.-> S2

  %% Styling
  classDef actor fill:#ffffff,stroke:#333,stroke-width:1px,color:#111;
  classDef usecase fill:#f7f7f7,stroke:#333,stroke-width:1px,color:#111;

  class OIDFC,SvcOp,Registry actor;
  class R1,TA1,TA2,TA3,I1,I2,TM1,TM2,TM3,C1,C2,C3,S1,S2,S3 usecase;
```


## Managed Mode

When this service is running in managed mode it loads its instance configuration from a given registry.

Loading of managed configuration happens _after_ startup, which means application will not be ready for traffic
immediately. The application offers a ready state endpoint to help orchestration tools to know when the service is
ready for traffic.

### Instance groups

To load modules from the registry the service needs to have an instance-id configured.
This informs the service about what instance group it belongs to. Multiple instance that should be configured in the same way and loadbalanced **should**
share the same instance id.

E.g. Pseudo Configuration of 4 nodes that is divided into two instance groups

```mermaid
graph LR
    subgraph Instance-987
        subgraph node-3
            123-tmi-1("/tmi");
        end
        subgraph node-4
            123-tmi-2("/tmi");
        end
    end
    subgraph Instance-123
        subgraph node-1
            123-ta-1("/ta");
        end
        subgraph node-2
            123-ta-2("/ta");
        end
    end
    loadbalancer("Loadbalancer /ta | /tmi") --> 123-ta-1 & 123-ta-2;
    loadbalancer --> 123-tmi-1 & 123-tmi-2;
```

## Modules

The service is split into multiple modules which is a group of OpenID Federation endpoints.
Each module _has_ to belong to a given entity that is present in properties or registry.

### TrustAnchor / Intermediate

#### Subordinate Listing

`GET /subordinate_listing`

*Query Parameters*

| Required | Name          | Description                                                            |
|----------|---------------|------------------------------------------------------------------------|
| No       | entity_type   | Filters the response on entity type                                    |
| No       | trust_marked  | Filters the response with entities that contain ATLEAST one Trust Mark |
| No       | trust_mark_type | Filters the response with entities containing a specific Trust Mark    |
| No       | intermediate  | Filters the response to only contain intermediates.                    |

#### Fetch

`GET /fetch`

*Query Parameters*

| Required | Name | Description                                                        |
|----------|------|--------------------------------------------------------------------|
| Yes      | sub  | The subject for which the Subordinate Statement is being requested |

### TrustMark

#### Trust Mark Endpoint

`GET /trust_mark`

*Query Parameters*

| Required | Name          | Description                                       |
|----------|---------------|---------------------------------------------------|
| Yes      | trust_mark_type | Trust Mark identifier                             |
| Yes      | sub           | The subject for which the Trust Mark is issued to |

#### Trust Mark Status

`GET /trust_mark_status`

*Query Parameters*

| Required | Name          | Description                                                    |
|----------|---------------|----------------------------------------------------------------|
| Yes      | trust_mark_type | Trust Mark identifier                                          |
| Yes      | sub           | The subject for which the Trust Mark is issued to              |
| No       | iat           | Seconds Since the Epoch. Time when this Trust Mark was issued. |

#### Trust Mark Listing

`GET /trust_mark_listing`

*Query Parameters*

| Required | Name          | Description                                 |
|----------|---------------|---------------------------------------------|
| Yes      | trust_mark_type | Trust Mark identifier                       |
| No       | sub           | Filter response to only contain this entity |

### Resolver

#### Resolve

`GET /resolve`

*Query Parameters*

| Required | Name         | Description                                                    |
|----------|--------------|----------------------------------------------------------------|
| Yes      | sub          | Subject to resolve                                             |
| Yes      | trust_anchor | Trust Anchor to resolve via                                    |
| No       | entity_type  | Filter response to only contain entities with this entity type |

#### Discovery

`GET /discovery`

*Query Parameters*

| Required | Name          | Description                                                    |
|----------|---------------|----------------------------------------------------------------|
| Yes      | trust_anchor  | Trust Anchor to resolve via                                    |
| No       | entity_type   | Filter response to only contain entities with this entity type |
| No       | trust_mark_type | Filter response to only contain entities with this trust mark  |

## Creating a release

Releases are created by Github-actions on tagged commits.

e.g.

```bash
git tag v0.0.0
git push origin v0.0.0
```

Will result in a release and a docker image with that tag.
