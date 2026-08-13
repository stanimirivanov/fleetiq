# Shared Protobuf Contracts

Owns versioned protobuf messages and gRPC service definitions shared by FleetIQ
modules. Maven generates Java messages, standard gRPC stubs, and Mutiny stubs.

- Additive changes are preferred.
- Field numbers and enum numeric values are permanent.
- Breaking changes require a new versioned package or explicitly reviewed baseline.
- Verify: `mvn -pl proto test`.

The descriptor compatibility baseline and update procedure are documented in
[`src/test/resources/contract/README.md`](src/test/resources/contract/README.md).
