# Deployment Ownership

Workload manifests remain under `infra/kubernetes/base/services/<workload>` for now, but each directory is an independently buildable Kustomize base and is owned with the corresponding deployable module.

Service-owned resources include the workload, Service, ConfigMap, probes, resource defaults, service-specific routes, and service-specific policies. Platform-owned resources include namespaces, shared databases, MQTT, Keycloak, observability, mesh defaults, and global authorization policy.

Platform resources are explicitly grouped in `infra/kubernetes/base/kustomization.yaml`. Each workload directory has its own independently buildable base. The root base composes platform resources plus those workload bases, and environment overlays remain the single complete deployment entry point.

Platform resources should only be moved into a nested platform base when their files are physically moved with it; Kustomize's default load restrictions reject child bases that reach into parent directories. Workload manifests can later move beside source code without changing ownership or composition semantics; only root Kustomize resource paths need to change.
