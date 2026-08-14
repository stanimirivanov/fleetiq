# Documentation Guidelines

FleetIQ is both a reference implementation and a portfolio project.
Documentation should explain architectural intent without repeating code that
already reads clearly.

## Javadoc

Add Javadoc to types that define a boundary or hide an important operational
rule:

- inbound and outbound ports;
- application services with non-obvious orchestration;
- transport adapters with delivery or acknowledgement semantics;
- persistence adapters with transaction, ordering, or consistency guarantees;
- shared security and actor APIs.

Document public methods only when callers need information that the signature
cannot express, such as ordering, idempotency, transaction ownership, expected
empty results, or failure behavior. Do not add Javadoc to generated code,
persistence entities, plain records, enums, mapper methods, constructors,
getters, or obvious application entry points.

## Module READMEs

Each deployable module owns a README covering its responsibility, boundaries,
data, runtime dependencies, verification command, and current limitations.
Shared modules such as `proto` and `security-common` need shorter
consumer-oriented READMEs. Cross-cutting decisions remain in `docs/`; module
READMEs link to them instead of copying them.

## Additional records

Use architecture decision records in `docs/adr/` when a decision has meaningful
alternatives or long-term consequences. Operational procedures belong in
runbooks, and externally consumed APIs should be documented from their protobuf
contracts.
