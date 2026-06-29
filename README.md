# Java AST SSOT

Part of **[Anchor Migration](https://github.com/anchor-migration/migration-hub)** — generic **Java source AST** exporter with optional stack profiles.

> [ADR-002 — core vs stack profiles](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-002-java-ast-ssot-core-and-profiles.md)  
> [ADR-004 — crosswalk contract](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-004-crosswalk-contract-mapping-roles-and-edge-kinds.md)

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
├── crosswalk/            # Link code SSOT ↔ schema SSOT (ADR-004)
├── listusage/            # On-demand list classifier (ADR-008 M2)
└── cli/                  # export, classify-lists, crosswalk, info, profiles
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

# Link code SSOT to schema SSOT (after db-metadata export)
# Alignment (ADR-005): bidirectional color_forward / color_backward on each link
java -jar target/java-ast-ssot-1.0.0-SNAPSHOT.jar crosswalk \
  --code-db metadata/dukesbank-code.db \
  --schema-db metadata/dukesbank.db \
  --db-schema dukesbank \
  -o metadata/dukesbank-linked.db

# On-demand list usage classifier (ADR-008 M2 — no cache)
java -jar target/java-ast-ssot-1.0.0-SNAPSHOT.jar classify-lists \
  -s /path/to/bank/src \
  --paths com/sun/ebank/ejb/account/AccountControllerBean.java \
  -o /tmp/list-usage.json
```

See [docs/list-usage-classifier.md](docs/list-usage-classifier.md).

## Schema

| Layer | DDL | Tables |
|-------|-----|--------|
| Core | `schema/core/v1.sql` | `export_run`, `java_type`, `java_method`, …, `source_comment` |
| Profile | `schema/profiles/javaee-ejb2-jboss/v1.sql` | `javaee_ejb2_jboss_*` |
| Crosswalk (link output) | `schema/crosswalk/v1.sql` | `crosswalk_run`, `code_schema_link` (+ alignment columns), `crosswalk_issue` |

### Sidecars (core)

| Sidecar | Table / tool | Purpose |
|---------|--------------|---------|
| Comments | `source_comment` | Raw comment blocks (`line`, `block`, `javadoc`); no v1 semantic links to AST nodes — [ADR-003](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-003-ast-sidecar-vs-lst-rewrite-layer.md) |

### On-demand analysis (not core SQLite)

| Tool | Output | Purpose |
|------|--------|---------|
| `classify-lists` | Ephemeral JSON | ADR-008 M2 — raw collection `homogeneous` / `tuple` / `unknown`; consumed by `rewrite-recipes` L2/L3 — [docs/list-usage-classifier.md](docs/list-usage-classifier.md) |

## Build

```bash
mvn test package
```

Without local Maven (Docker):

```bash
docker run --rm -v "$PWD:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -B test package
```

Duke's Bank end-to-end (MySQL in `demo-dukesbank` Docker Compose, JDBC on `localhost:3306`):

```bash
# 1. Schema SSOT (host Python db-metadata → Docker MySQL)
db-migration export --url "mysql+pymysql://dukesbank:dukesbank@localhost:3306/dukesbank" \
  --out metadata/dukesbank.db

# 2–4. Build + export + crosswalk (Docker Maven; mount bank source separately)
# See demo-dukesbank/README.md for full commands and Windows paths.
```

On Windows PowerShell, use `C:/github/anchor-migration/java-ast-ssot` as the mount path instead of `$PWD` if needed.

## License

MIT
