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

- **L2** — `HomogeneousRawListTyping` reads homogeneous sites; `failOnTupleList` skips tuple sites ([homogeneous-raw-list-l2.md](https://github.com/anchor-migration/rewrite-recipes/blob/main/docs/homogeneous-raw-list-l2.md))
- **L3** — `TupleListToResultClass` targets tuple sites; proposal-first workflow ([tuple-list-l3.md](https://github.com/anchor-migration/rewrite-recipes/blob/main/docs/tuple-list-l3.md))
- **Human review** — run before enabling L2/L3 on broad codebases

See [ADR-008](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-008-java-language-modernization-and-tuple-lists.md).
