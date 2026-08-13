# FleetIQ Testing Strategy

Tests are owned by the module whose contract they protect.

1. **Architecture tests** run in every service and enforce package dependency direction.
2. **Domain tests** cover invariants and explicit business outcomes without Quarkus.
3. **Adapter mapping tests** cover transport/domain conversion and status mapping.
4. **Repository contract tests** run the same behavioral contract against each real adapter using its actual database extension.
5. **Security contract tests** cover unauthenticated, wrong-role, missing-tenant, valid-tenant, and cross-tenant calls.
6. **Migration tests** prove both a clean install and supported upgrade paths.
7. **Integration tests** use Testcontainers for MQTT, PostgreSQL extensions, and Keycloak where applicable.
8. **End-to-end tests** are few and cover critical flows across deployables.

Unit tests use `*Test`. Container-backed tests use `*IT` and belong to the Maven Failsafe phase once the shared integration-test harness is introduced. Tests must not hide unavailable Docker by reporting success; they should either execute or be explicitly skipped by a documented profile.
