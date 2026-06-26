PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS meta_schema_version (
    version     INTEGER PRIMARY KEY,
    applied_at  TEXT NOT NULL DEFAULT (datetime('now'))
);
INSERT OR IGNORE INTO meta_schema_version (version) VALUES (1);

CREATE TABLE IF NOT EXISTS export_run (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    source_root         TEXT NOT NULL,
    exported_at         TEXT NOT NULL DEFAULT (datetime('now')),
    tool_version        TEXT NOT NULL,
    java_file_count     INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS source_file (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    relative_path   TEXT NOT NULL,
    file_kind       TEXT NOT NULL CHECK (file_kind IN ('java', 'xml')),
    UNIQUE (export_run_id, relative_path)
);

CREATE TABLE IF NOT EXISTS java_type (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    source_file_id  INTEGER NOT NULL REFERENCES source_file(id),
    stable_id       TEXT NOT NULL,
    package_name    TEXT,
    simple_name     TEXT NOT NULL,
    kind            TEXT NOT NULL CHECK (kind IN ('class', 'interface', 'enum', 'annotation', 'record')),
    extends_type    TEXT,
    implements_list TEXT,
    UNIQUE (export_run_id, stable_id)
);

CREATE TABLE IF NOT EXISTS java_method (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    type_id         INTEGER NOT NULL REFERENCES java_type(id),
    stable_id       TEXT NOT NULL,
    name            TEXT NOT NULL,
    return_type     TEXT,
    modifiers       TEXT,
    UNIQUE (export_run_id, stable_id)
);

CREATE TABLE IF NOT EXISTS java_field (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    type_id         INTEGER NOT NULL REFERENCES java_type(id),
    stable_id       TEXT NOT NULL,
    name            TEXT NOT NULL,
    field_type      TEXT,
    modifiers       TEXT,
    UNIQUE (export_run_id, stable_id)
);

CREATE TABLE IF NOT EXISTS java_import (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    source_file_id  INTEGER NOT NULL REFERENCES source_file(id),
    imported_name   TEXT NOT NULL,
    is_static       INTEGER NOT NULL DEFAULT 0,
    is_wildcard     INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ejb_bean (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    descriptor_file TEXT NOT NULL,
    ejb_name        TEXT NOT NULL,
    ejb_class       TEXT,
    bean_type       TEXT NOT NULL CHECK (bean_type IN ('entity', 'session', 'message')),
    session_type    TEXT,
    persistence_type TEXT,
    table_name      TEXT,
    UNIQUE (export_run_id, ejb_name)
);

CREATE TABLE IF NOT EXISTS ejb_cmp_field (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    ejb_name        TEXT NOT NULL,
    field_name      TEXT NOT NULL,
    column_name     TEXT,
    UNIQUE (export_run_id, ejb_name, field_name)
);

CREATE TABLE IF NOT EXISTS ejb_ref (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    source_ejb_name TEXT NOT NULL,
    ref_name        TEXT,
    ref_type        TEXT,
    ejb_link        TEXT,
    ref_kind        TEXT NOT NULL CHECK (ref_kind IN ('ejb-local-ref', 'ejb-ref', 'resource-ref'))
);

CREATE TABLE IF NOT EXISTS crosswalk_edge (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    edge_kind       TEXT NOT NULL,
    source_stable_id TEXT NOT NULL,
    target_stable_id TEXT NOT NULL,
    UNIQUE (export_run_id, edge_kind, source_stable_id, target_stable_id)
);

CREATE INDEX IF NOT EXISTS idx_java_type_stable ON java_type (export_run_id, stable_id);
CREATE INDEX IF NOT EXISTS idx_ejb_bean_name ON ejb_bean (export_run_id, ejb_name);
