package com.anchor.migration.javaastssot.crosswalk;

import com.anchor.migration.javaastssot.crosswalk.schema.SchemaSsotReader;

import java.sql.Connection;

public record CrosswalkContext(
        Connection codeConn,
        Connection schemaConn,
        int codeExportRunId,
        int schemaExportRunId,
        String dbSchema,
        SchemaSsotReader schemaReader) {}
