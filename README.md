# Java AST SSOT

Part of **[Anchor Migration](https://github.com/anchor-migration/migration-hub)** — export **Java source structure** to SQLite SSOT.

> **Positioning:** [ADR-002 — core vs stack profiles](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-002-java-ast-ssot-core-and-profiles.md)

## What this repo is

| | |
|--|--|
| **Product** | Generic **Java** AST exporter (types, methods, fields, imports) |
| **Not** | A Duke's Bank–only or Java EE–only tool |
| **Duke's Bank** | First **reference demo** + first **stack profile** validation (`javaee-ejb2-jboss`) |

Naming is **`{language}-ast-ssot`**. Future repos (e.g. `cobol-ast-ssot`) cover other languages for heterogeneous migration; linking across languages is a separate layer.

## Architecture (target: Option A)

```
┌─────────────────────────────────────────┐
│  java-ast-ssot CORE (always)            │
│  JavaParser → java_type, method, field… │
└─────────────────┬───────────────────────┘
                  │ optional --profile
        ┌─────────┴─────────┐
        ▼                   ▼
 javaee-ejb2-jboss      spring / jpa …
 (ejb-jar, jbosscmp)    (future profiles)
```

Same pattern as **`db-metadata`**: core export + dialect/profile adapters.

## v0.1 POC (current implementation)

**Documentation reflects target design; code is not refactored yet.**

| Layer | v0.1 behavior |
|-------|----------------|
| **Core** | JavaParser on all `.java` under `--source-root` |
| **Profile** | EJB/JBoss XML parsers **always run** when `ejb-jar.xml` / `jbosscmp-jdbc.xml` are found (implicit `javaee-ejb2-jboss`) |

Step 2 refactor will add explicit `--profile` and allow core-only export without EJB tables.

### Core scope

- JavaParser — types, methods, fields, imports (Java 1.4 language level today)
- SQLite tables: `java_type`, `java_method`, `java_field`, `java_import`, `source_file`

### Profile: `javaee-ejb2-jboss` (Duke's Bank validated)

- `ejb-jar.xml`, `jbosscmp-jdbc.xml`
- Tables: `ejb_bean`, `ejb_cmp_field`, `ejb_ref`, `crosswalk_edge`
- Crosswalk kinds: `java_type_to_ejb`, `ejb_to_table`

Comments → separate layer, not in v0.1.

## Build

Requires JDK 17+ and Maven 3.9+.

```bash
cd java-ast-ssot
mvn -q package
```

Fat JAR: `target/java-ast-ssot-0.1.0-SNAPSHOT.jar`

Docker build:

```bash
docker run --rm -v "$PWD:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -q package
```

## Duke's Bank example (profile validation)

External clone as sibling of `anchor-migration` — see [demo-dukesbank](../demo-dukesbank).

```bash
java -jar target/java-ast-ssot-0.1.0-SNAPSHOT.jar export \
  --source-root "C:/github/dukesbank/src/j2eetutorial14/examples/bank" \
  --out metadata/dukesbank-code.db

java -jar target/java-ast-ssot-0.1.0-SNAPSHOT.jar info \
  --db metadata/dukesbank-code.db
```

**Verified (2026-06-27):** 61 Java files, 61 types, 406 methods; with implicit Java EE profile: 8 EJB beans, 8 crosswalk edges.

A plain Spring or Java SE tree should only populate **core** tables after refactor; today EJB tables stay empty if no descriptors are present.

## SQLite schema

See [`src/main/resources/schema/v1.sql`](src/main/resources/schema/v1.sql).

Schema v1 mixes core + Java EE profile tables — **will split in refactor** (ADR-002 Step 2).

## License

MIT
