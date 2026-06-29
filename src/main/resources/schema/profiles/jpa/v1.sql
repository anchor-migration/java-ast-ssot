-- Profile jpa: JPA @Entity / @Table / @Column binding facts (ADR-004 Step 4)

CREATE TABLE IF NOT EXISTS jpa_entity (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    source_file     TEXT NOT NULL,
    type_stable_id  TEXT NOT NULL,
    table_name      TEXT NOT NULL,
    UNIQUE (export_run_id, type_stable_id)
);

CREATE TABLE IF NOT EXISTS jpa_field (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    type_stable_id  TEXT NOT NULL,
    field_name      TEXT NOT NULL,
    column_name     TEXT NOT NULL,
    id_field        INTEGER NOT NULL DEFAULT 0,
    UNIQUE (export_run_id, type_stable_id, field_name)
);

CREATE INDEX IF NOT EXISTS idx_jpa_entity_type
    ON jpa_entity (export_run_id, type_stable_id);
