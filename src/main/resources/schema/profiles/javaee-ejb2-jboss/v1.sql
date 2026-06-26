-- Profile javaee-ejb2-jboss: EJB 2.x + JBoss CMP deployment descriptors

CREATE TABLE IF NOT EXISTS javaee_ejb2_jboss_bean (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id    INTEGER NOT NULL REFERENCES export_run(id),
    descriptor_file  TEXT NOT NULL,
    ejb_name         TEXT NOT NULL,
    ejb_class        TEXT,
    bean_type        TEXT NOT NULL CHECK (bean_type IN ('entity', 'session', 'message')),
    session_type     TEXT,
    persistence_type TEXT,
    table_name       TEXT,
    UNIQUE (export_run_id, ejb_name)
);

CREATE TABLE IF NOT EXISTS javaee_ejb2_jboss_cmp_field (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id INTEGER NOT NULL REFERENCES export_run(id),
    ejb_name      TEXT NOT NULL,
    field_name    TEXT NOT NULL,
    column_name   TEXT,
    UNIQUE (export_run_id, ejb_name, field_name)
);

CREATE TABLE IF NOT EXISTS javaee_ejb2_jboss_ref (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    source_ejb_name TEXT NOT NULL,
    ref_name        TEXT,
    ref_type        TEXT,
    ejb_link        TEXT,
    ref_kind        TEXT NOT NULL CHECK (ref_kind IN ('ejb-local-ref', 'ejb-ref', 'resource-ref'))
);

CREATE TABLE IF NOT EXISTS javaee_ejb2_jboss_crosswalk (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id    INTEGER NOT NULL REFERENCES export_run(id),
    edge_kind        TEXT NOT NULL,
    source_stable_id TEXT NOT NULL,
    target_stable_id TEXT NOT NULL,
    UNIQUE (export_run_id, edge_kind, source_stable_id, target_stable_id)
);

CREATE INDEX IF NOT EXISTS idx_javaee_ejb2_jboss_bean_name
    ON javaee_ejb2_jboss_bean (export_run_id, ejb_name);
