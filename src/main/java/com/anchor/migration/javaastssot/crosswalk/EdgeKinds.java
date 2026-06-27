package com.anchor.migration.javaastssot.crosswalk;

public final class EdgeKinds {
    public static final String TYPE_MAPS_TO_TABLE = "type_maps_to_table";
    public static final String FIELD_MAPS_TO_COLUMN = "field_maps_to_column";
    public static final String FIELD_MAPS_TO_COLUMN_VIA = "field_maps_to_column_via";
    public static final String TYPE_BACKED_BY_SQL = "type_backed_by_sql";
    public static final String METHOD_EXECUTES_SQL = "method_executes_sql";
    public static final String SQL_REFERENCES_TABLE = "sql_references_table";
    public static final String RELATIONSHIP_MAPS_TO_TABLE = "relationship_maps_to_table";
    public static final String STACK_BRIDGE = "stack_bridge";

    private EdgeKinds() {}
}
