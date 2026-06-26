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
    java_file_count     INTEGER NOT NULL DEFAULT 0,
    profiles            TEXT NOT NULL DEFAULT ''
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

CREATE INDEX IF NOT EXISTS idx_java_type_stable ON java_type (export_run_id, stable_id);
