PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS meta_crosswalk_version (
    version     INTEGER PRIMARY KEY,
    applied_at  TEXT NOT NULL DEFAULT (datetime('now'))
);
INSERT OR IGNORE INTO meta_crosswalk_version (version) VALUES (2);

CREATE TABLE IF NOT EXISTS crosswalk_run (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    linked_at               TEXT NOT NULL DEFAULT (datetime('now')),
    tool_version            TEXT NOT NULL,
    code_db_path            TEXT NOT NULL,
    schema_db_path          TEXT NOT NULL,
    code_export_run_id      INTEGER NOT NULL,
    schema_export_run_id    INTEGER NOT NULL,
    db_schema               TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS code_schema_link (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    crosswalk_run_id        INTEGER NOT NULL REFERENCES crosswalk_run(id),
    edge_kind               TEXT NOT NULL,
    source_stable_id        TEXT NOT NULL,
    target_stable_id        TEXT NOT NULL,
    mapping_role            TEXT NOT NULL,
    profile_id              TEXT NOT NULL,
    binding_source          TEXT NOT NULL,
    evidence_ref            TEXT,
    confidence              TEXT NOT NULL CHECK (confidence IN ('authoritative', 'inferred')),
    name_drift_class        TEXT NOT NULL DEFAULT 'none',
    type_relation_forward   TEXT NOT NULL DEFAULT 'unknown',
    type_relation_backward  TEXT NOT NULL DEFAULT 'unknown',
    color_forward           TEXT NOT NULL DEFAULT 'green',
    color_backward          TEXT NOT NULL DEFAULT 'green',
    round_trip_class        TEXT NOT NULL DEFAULT 'safe',
    normalized_source       TEXT,
    normalized_target       TEXT,
    UNIQUE (crosswalk_run_id, edge_kind, source_stable_id, target_stable_id, profile_id)
);

CREATE INDEX IF NOT EXISTS idx_code_schema_link_run
    ON code_schema_link (crosswalk_run_id, edge_kind);

CREATE TABLE IF NOT EXISTS crosswalk_issue (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    crosswalk_run_id        INTEGER NOT NULL REFERENCES crosswalk_run(id),
    severity                TEXT NOT NULL CHECK (severity IN ('error', 'warning')),
    issue_code              TEXT NOT NULL,
    message                 TEXT NOT NULL,
    context_ref             TEXT
);
