# Fleet Topology

Maintains an eventually consistent local projection of vehicles, positions, and their
relationships. It serves graph traversal and proximity queries without synchronous
calls to source services.

- Inbound boundaries: authenticated topology gRPC API plus device and position MQTT events.
- Persistence: relational timestamp gates and query projections alongside Apache AGE vertices and edges.
- Consistency: duplicate and stale events are ignored using independent device and position timestamps.
- Queries: bounded undirected traversal and distance-ordered proximity search.
- Tenant isolation: event projections, relational keys, AGE vertices and edges, traversals,
  and proximity searches are scoped by tenant.
- Verify: `mvn -pl services/fleet-topology -am verify`.

The relational projection provides predictable parameterized queries; AGE remains the
graph representation and is synchronized in the same PostgreSQL transaction.
