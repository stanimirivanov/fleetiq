# FleetIQ Documentation

This directory contains information that applies across module boundaries. Start with
the repository README for setup, then use the following documents as needed:

- [Implementation roadmap](implementation-roadmap.md) — phased goals, acceptance criteria, and trackable tasks.
- [AI model baseline](ai-models.md) — approved local models, versions, dimensions, and operating constraints.

- [Architecture baseline](architecture-baseline.md) — module boundaries and dependency rules.
- [Testing strategy](testing-strategy.md) — test layers, ownership, and commands.
- [Deployment ownership](deployment-ownership.md) — where Kubernetes and deployment assets belong.
- [Documentation guidelines](documentation-guidelines.md) — what to document and what to leave to code.

Module-specific runtime details live beside the module in its `README.md`. Decisions
that constrain future implementations should be captured as architecture decision
records under `docs/adr/`; operational recovery procedures should go under
`docs/runbooks/` when those procedures are introduced.
