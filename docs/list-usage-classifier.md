# List usage classifier (ADR-008 M2)

On-demand, **ephemeral** analysis of raw `ArrayList` / `Vector` / `List` sites. No SQLite sidecar, no cache.

## Usage

```bash
java -jar target/java-ast-ssot-1.0.0-SNAPSHOT.jar classify-lists \
  -s /path/to/src \
  --paths com/example/Foo.java \
  -o /tmp/list-usage.json
```

Omit `--paths` to scan all `.java` files under `--source-root`. Omit `-o` to print JSON to stdout; summary goes to stderr.

## Classification

| `usageClass` | Rule |
|--------------|------|
| `homogeneous` | All `add` / `get+cast` evidence agrees on one element type |
| `tuple` | Same site shows ≥2 incompatible types |
| `unknown` | No usable evidence (unused site omitted from report) |

Scope: **intra-procedural** only. Cross-method flow → not tracked (future: mark unknown at L2 gate).

## Consumers

- **L2 recipes** (future): read JSON report; `failOnTupleList`
- **Human review**: run before enabling homogeneous generic typing

See [ADR-008 M2](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-008-java-language-modernization-and-tuple-lists.md).
