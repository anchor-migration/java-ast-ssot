# Java AST SSOT

Part of **[Anchor Migration](https://github.com/anchor-migration/migration-hub)** — export **Java** sources and EJB deployment descriptors to SQLite DRG SSOT.

> Program map: [START-HERE.md](https://github.com/anchor-migration/migration-hub/blob/main/docs/START-HERE.md)

## Naming (language-specific on purpose)

This repository is **`java-ast-ssot`**, not a generic `code-ast-ssot`. Anchor Migration treats each legacy **language** as its own extractor repo with the same SSOT pattern (deterministic export → SQLite → crosswalk → verify).

| Repo (current / future) | Scope |
|-------------------------|--------|
| **`java-ast-ssot`** | Java sources, EJB/JBoss XML |
| `db-metadata` | Relational schema (any supported DB) |
| *(reserved)* e.g. `cobol-ast-ssot` | COBOL → AST SSOT for heterogeneous migration |

Heterogeneous paths (e.g. COBOL → Java) reuse the **same principles** — separate language SSOTs plus a linking layer — but are **not** implemented here.

## Scope (v0.1 POC)

- **JavaParser** — types, methods, fields, imports (Java 1.4 language level)
- **XML** — `ejb-jar.xml`, `jbosscmp-jdbc.xml`
- **Crosswalk edges** — `java_type_to_ejb`, `ejb_to_table`
- Comments stored separately — **not in v0.1**

## Build

Requires JDK 17+ and Maven 3.9+.

```bash
cd java-ast-ssot
mvn -q package
```

Fat JAR: `target/java-ast-ssot-0.1.0-SNAPSHOT.jar`

Without local Maven/JDK, build via Docker:

```bash
docker run --rm -v "$PWD:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -q package
```

## Duke's Bank example

```bash
java -jar target/java-ast-ssot-0.1.0-SNAPSHOT.jar export \
  --source-root "C:/github/dukesbank/src/j2eetutorial14/examples/bank" \
  --out metadata/dukesbank-code.db

java -jar target/java-ast-ssot-0.1.0-SNAPSHOT.jar info \
  --db metadata/dukesbank-code.db
```

Scans all `.java` files under the root plus `ejb-jar.xml` / `jbosscmp-jdbc.xml` anywhere in the tree.

**Verified (2026-06-27):** 61 Java files, 61 types, 406 methods, 8 EJB beans, 8 crosswalk edges (4 CMP entities linked to tables).

## SQLite schema

See [`src/main/resources/schema/v1.sql`](src/main/resources/schema/v1.sql).

Core tables: `java_type`, `java_method`, `java_field`, `ejb_bean`, `ejb_cmp_field`, `crosswalk_edge`.

## Design

See [DUKESBANK-DEMO.md](https://github.com/anchor-migration/migration-hub/blob/main/docs/DUKESBANK-DEMO.md) — AST for SSOT, LST deferred to OpenRewrite recipes.

## License

MIT
