# Protobuf compatibility baseline

`fleetiq-api.pb` captures the last accepted public protobuf contract. The
compatibility test allows additive messages, fields, enum values, services, and
RPCs, while rejecting source- or wire-incompatible changes to existing API
elements.

Update the baseline only as an explicit contract decision. From the repository
root, with `protoc` on `PATH`, run:

```powershell
protoc -I=proto/src/main/proto --include_imports `
  --descriptor_set_out=proto/src/test/resources/contract/fleetiq-api.pb `
  (Get-ChildItem proto/src/main/proto -Recurse -Filter *.proto).FullName
```

Commit the protobuf changes and refreshed baseline together so the breaking
change is visible during review.
