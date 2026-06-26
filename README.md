# Java AST SSOT

Part of **[Anchor Migration](https://github.com/anchor-migration/migration-hub)** — generic **Java source AST** exporter with optional stack profiles.

> [ADR-002 — core vs stack profiles](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-002-java-ast-ssot-core-and-profiles.md)

**Breaking release:** `1.0.0-SNAPSHOT` — no compatibility with v0.x SQLite files or package names.

## Package layout

```
com.anchor.migration.javaastssot
├── core/                 # Always runs
│   ├── extract/          # JavaParser
│   ├── model/            # java_type, method, field, …
│   └── store/            # SQLite core schema + writer
├── profile/              # Optional stack adapters
│   └── javaee/ejb2jboss/ # Profile javaee-ejb2-jboss
└── cli/                  # export, info, profiles
```

## CLI

```bash
# Core only
java -jar target/java-ast-ssot-1.0.0-SNAPSHOT.jar export \
  -s /path/to/src -o metadata/java.db

# Duke's Bank (Java EE profile)
java -jar target/java-ast-ssot-1.0.0-SNAPSHOT.jar export \
  -s /path/to/bank \
  --profile javaee-ejb2-jboss \
  -o metadata/dukesbank-code.db

java -jar target/java-ast-ssot-1.0.0-SNAPSHOT.jar profiles
java -jar target/java-ast-ssot-1.0.0-SNAPSHOT.jar info -d metadata/java.db
```

## Schema

| Layer | DDL | Tables |
|-------|-----|--------|
| Core | `schema/core/v1.sql` | `export_run`, `java_type`, `java_method`, … |
| Profile | `schema/profiles/javaee-ejb2-jboss/v1.sql` | `javaee_ejb2_jboss_*` |

Profile tables are created **only** when that profile is enabled for an export.

## Build

```bash
mvn test package
```

## License

MIT
