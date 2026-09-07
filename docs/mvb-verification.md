# MVB Verification Record

## Scope

This record captures the evidence used to declare the FleetIQ minimum viable
baseline complete. It covers the locally reproducible portfolio system defined in
the [MVB release gate](mvb-release.md); it does not claim production readiness for
the deferred roadmap.

## Verification environment

- Date: 2026-09-07
- Operating system: Windows 11 x86-64 host
- Java: Oracle JDK 25.0.4 LTS
- Maven: 3.9.16
- Docker Desktop engine: 29.6.2
- Quarkus: 3.38.1
- Apache Pekko: 1.3.0

Oracle JDK 25.0.4 crashed once inside HotSpot while JIT-compiling `javac` on this
workstation. The successful release run disabled tiered compilation through
`MAVEN_OPTS`; this changes JVM compilation strategy, not project behavior or the
tests being executed.

## Release commands

From the repository root in PowerShell:

```powershell
$env:MAVEN_OPTS='-XX:-TieredCompilation'
mvn clean verify
docker compose -f infra/docker-compose/docker-compose.yml config --quiet
./scripts/run-mvb-telemetry-demo.ps1 -SkipBuild -TimeoutSeconds 120
git diff --check
```

## Results

- Full Maven reactor: passed; all 11 modules succeeded in 10 minutes 10 seconds.
- Unit tests: 87 passed, 0 failed, 0 errored, 0 skipped.
- Integration tests: 25 passed, 0 failed, 0 errored, 0 skipped.
- Total: 112 tests across 41 XML reports.
- Compose configuration: valid.
- Black-box journey: passed — simulator → secured MQTT → `telemetry_db` →
  transactional position outbox/MQTT → `topology_db`.
- Black-box persisted evidence: one new telemetry sample and one matching topology
  projection were observed during the bounded run.
- Markdown relative-link audit: all links resolved.
- Git whitespace validation: passed.

The black-box run used the actual simulator and packaged Telemetry Ingestion and
Fleet Topology services. Its captured logs were written under `target/mvb-demo/`,
which is build output and is intentionally not versioned.

## Known non-blocking observations

- Several upstream libraries emit Java 25 deprecation/native-access warnings.
- Quarkus tests may report duplicate `junit-platform.properties` resources and
  disabled test messaging channels; these do not fail the suites.
- Testcontainers reusable databases can retain migrated schemas between test JVMs;
  migration tests verify the final migration count and required objects.
- Kubernetes manifests remain an unverified future deployment baseline. Database
  extension-image distribution and manifest policy validation are explicitly
  deferred in the roadmap.
