-- Profile mybatis: mapper XML binding facts (ADR-004 Step 5)

CREATE TABLE IF NOT EXISTS mybatis_result_map (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    source_file     TEXT NOT NULL,
    result_map_id   TEXT NOT NULL,
    type_stable_id  TEXT NOT NULL,
    mapping_role    TEXT NOT NULL,
    table_name      TEXT,
    statement_id    TEXT,
    UNIQUE (export_run_id, source_file, result_map_id)
);

CREATE TABLE IF NOT EXISTS mybatis_result_field (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    source_file     TEXT NOT NULL,
    result_map_id   TEXT NOT NULL,
    property_name   TEXT NOT NULL,
    column_name     TEXT NOT NULL,
    table_name      TEXT,
    UNIQUE (export_run_id, source_file, result_map_id, property_name)
);

CREATE TABLE IF NOT EXISTS mybatis_statement (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id           INTEGER NOT NULL REFERENCES export_run(id),
    source_file             TEXT NOT NULL,
    statement_id            TEXT NOT NULL,
    statement_type          TEXT NOT NULL,
    result_map_id           TEXT,
    result_type_stable_id   TEXT,
    sql_text                TEXT NOT NULL,
    join_query              INTEGER NOT NULL DEFAULT 0,
    mapping_role            TEXT,
    UNIQUE (export_run_id, source_file, statement_id)
);

CREATE TABLE IF NOT EXISTS mybatis_statement_table (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    source_file     TEXT NOT NULL,
    statement_id    TEXT NOT NULL,
    table_name      TEXT NOT NULL,
    UNIQUE (export_run_id, source_file, statement_id, table_name)
);

CREATE INDEX IF NOT EXISTS idx_mybatis_result_map_type
    ON mybatis_result_map (export_run_id, type_stable_id);

CREATE INDEX IF NOT EXISTS idx_mybatis_statement_id
    ON mybatis_statement (export_run_id, source_file, statement_id);
