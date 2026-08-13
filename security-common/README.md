# Security Common

Provides the shared tenant-security interceptor, request-scoped tenant context, and
tenant identity value used by service inbound adapters.

This module establishes identity at the API boundary; it does not make persistence
tenant-safe by itself. Use `CurrentTenant` only after `@TenantSecured` interception,
and pass tenant identity explicitly into tenant-owned use cases and repositories.

Verify with `mvn -pl security-common test`.
