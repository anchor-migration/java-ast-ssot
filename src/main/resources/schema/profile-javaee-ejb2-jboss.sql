-- Profile: javaee-ejb2-jboss (EJB 2.x + JBoss CMP deployment descriptors)

CREATE TABLE IF NOT EXISTS ejb_bean (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    profile_id      TEXT NOT NULL DEFAULT 'javaee-ejb2-jboss',
    descriptor_file TEXT NOT NULL,
    ejb_name        TEXT NOT NULL,
    ejb_class       TEXT,
    bean_type       TEXT NOT NULL CHECK (bean_type IN ('entity', 'session', 'message')),
    session_type    TEXT,
    persistence_type TEXT,
    table_name      TEXT,
    UNIQUE (export_run_id, profile_id, ejb_name)
);

CREATE TABLE IF NOT EXISTS ejb_cmp_field (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    profile_id      TEXT NOT NULL DEFAULT 'javaee-ejb2-jboss',
    ejb_name        TEXT NOT NULL,
    field_name      TEXT NOT NULL,
    column_name     TEXT,
    UNIQUE (export_run_id, profile_id, ejb_name, field_name)
);

CREATE TABLE IF NOT EXISTS ejb_ref (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    profile_id      TEXT NOT NULL DEFAULT 'javaee-ejb2-jboss',
    source_ejb_name TEXT NOT NULL,
    ref_name        TEXT,
    ref_type        TEXT,
    ejb_link        TEXT,
    ref_kind        TEXT NOT NULL CHECK (ref_kind IN ('ejb-local-ref', 'ejb-ref', 'resource-ref'))
);

CREATE TABLE IF NOT EXISTS profile_crosswalk_edge (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    export_run_id   INTEGER NOT NULL REFERENCES export_run(id),
    profile_id      TEXT NOT NULL,
    edge_kind       TEXT NOT NULL,
    source_stable_id TEXT NOT NULL,
    target_stable_id TEXT NOT NULL,
    UNIQUE (export_run_id, profile_id, edge_kind, source_stable_id, target_stable_id)
);

CREATE INDEX IF NOT EXISTS idx_ejb_bean_name ON ejb_bean (export_run_id, profile_id, ejb_name);
