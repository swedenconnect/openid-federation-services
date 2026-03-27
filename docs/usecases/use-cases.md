# Use Cases

This document describes common use cases supported by OpenID Federation Services.

## Resolve and verify federation entity metadata

A client needs verified metadata for a federation entity in the context of a specific trust anchor.

The service resolves the federation chain, retrieves the required entity statements, validates signatures, verifies the trust chain, and returns a resolver result.

### Supporting API capabilities

#### Resolver API

- **Resolve and verify entity metadata**  
  Accepts a subject and a trust anchor, retrieves entity statements in the federation chain, and verifies signatures and trust before returning a resolver result.

## Discover entities in a federation

A client or operator needs to discover entities based on criteria such as entity type or trust mark.

The service supports discovery through federation data exposed by the trust anchor.

### Supporting API capabilities

#### Trust Anchor API

- **Discover entities**  
  Supports discovery of entities through the trust anchor based on criteria such as entity type or trust mark.

- **Get subordinate listing**  
  Returns a list of entities directly subordinated to the trust anchor, with optional filtering.

## Expose trust anchor federation data

A federation operator needs to expose information about entities directly subordinated to a trust anchor.

The service provides signed federation data through trust anchor endpoints.

### Supporting API capabilities

#### Trust Anchor API

- **Get subordinate listing**  
  Returns a list of entities directly subordinated to the trust anchor, with optional filtering.

- **Get subordinate statement**  
  Returns a signed entity statement for a specified subordinate.

## Expose intermediate federation data

A federation operator needs to expose information about entities subordinated to an intermediate node.

The service provides signed federation data through intermediate endpoints.

### Supporting API capabilities

#### Intermediate API

- **Get subordinate listing**  
  Returns a list of entities subordinated to the intermediate node.

- **Get subordinate statement**  
  Returns a signed statement for a subordinate under the intermediate node.

## Issue and manage trust marks

A federation authority or trust mark issuer needs to issue trust marks and provide status information for them.

The service supports issuance, status verification, and listing of trust marks.

### Supporting API capabilities

#### Trust Mark Issuer API

- **Get trust mark**  
  Returns an issued trust mark for a specified entity and trust mark type.

- **Verify trust mark status**  
  Checks the validity and current status of a trust mark, including revocation status.

- **List trust marks**  
  Returns an overview of trust marks, optionally filtered by type or entity.

## Configure and operate a federation service instance

An operator needs to configure, monitor, and run an instance of OpenID Federation Services.

The service supports module configuration, operational status endpoints, and registry-based configuration in managed mode.

### Supporting API capabilities

#### Configuration and status

- **Module configuration**  
  Loads and exposes configuration for the active federation modules in the instance.

- **Monitoring**  
  Exposes health, readiness, and status information for operations and orchestration.

- **Registry-based configuration**  
  Retrieves and applies instance configuration from the OpenID Federation Registry when running in managed mode.

## Shared supporting capabilities

These capabilities support multiple use cases across the service.

### Core support

- **Fetch external federation metadata**  
  Calls external federation endpoints and retrieves entity statements needed for resolve and discovery operations.

- **Verify signatures**  
  Validates JWT-based and JWK-based signatures for federation statements and trust marks.

- **Cache federation metadata**  
  Stores fetched and verified federation metadata in cache or another configured storage backend to improve performance and reduce dependency on external endpoints.