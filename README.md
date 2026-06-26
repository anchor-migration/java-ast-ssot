# Java AST SSOT

Part of **[Anchor Migration](https://github.com/anchor-migration/migration-hub)** — export **Java source structure** to SQLite SSOT.

> **Positioning:** [ADR-002 — core vs stack profiles](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-002-java-ast-ssot-core-and-profiles.md)

## What this repo is

| | |
|--|--|
| **Product** | Generic **Java** AST exporter (types, methods, fields, imports) |
| **Profiles** | Optional stack adapters (`javaee-ejb2-jboss`, …) |
| **Duke's Bank** | Reference demo validating the Java EE profile |

## CLI (v0.2)

```bash
# Core only — any Java project
java -jar target/java-ast-ssot-0.2.0-SNAPSHOT.jar export \
  --source-root /path/to/src \
  --out metadata/java.db

# Java EE EJB 2.x + JBoss CMP (Duke's Bank)
java -jar target/java-ast-ssot-0.2.0-SNAPSHOT.jar export \
  --source-root /path/to/bank \
  --profile javaee-ejb2-jboss \
  --out metadata/dukesbank-code.db

# Optional: auto-enable profiles when descriptor files are found
java -jar target/java-ast-ssot-0.2.0-SNAPSHOT.jar export \
  -s /path/to/bank -o metadata/dukesbank-code.db \
  --auto-detect-profiles

java -jar target/java-ast-ssot-0.2.0-SNAPSHOT.jar profiles
java -jar target/java-ast-ssot-0.2.0-SNAPSHOT.jar info --db metadata/java.db
```

## Architecture

```
CORE (always)     JavaParser → java_type, method, field, import
       +
PROFILE (opt)     --profile javaee-ejb2-jboss → ejb_*, profile_crosswalk_edge
```

Schema files:

- `src/main/resources/schema/v1-core.sql`
- `src/main/resources/schema/profile-javaee-ejb2-jboss.sql` (applied only when profile enabled)

## Build & test

```bash
mvn test package
```

Docker:

```bash
docker run --rm -v "$PWD:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -q test package
```

## Duke's Bank example

External `dukesbank` clone as sibling of `anchor-migration` — see [demo-dukesbank](../demo-dukesbank).

```bash
java -jar target/java-ast-ssot-0.2.0-SNAPSHOT.jar export \
  --source-root "C:/github/dukesbank/src/j2eetutorial14/examples/bank" \
  --profile javaee-ejb2-jboss \
  --out metadata/dukesbank-code.db
```

## License

MIT
