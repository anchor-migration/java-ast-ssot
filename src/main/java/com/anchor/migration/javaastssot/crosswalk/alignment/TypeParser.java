package com.anchor.migration.javaastssot.crosswalk.alignment;

import java.util.Locale;

final class TypeParser {

    private TypeParser() {}

    static LogicalType fromSqlDataType(String dataType) {
        if (dataType == null || dataType.isBlank()) {
            return LogicalType.UNKNOWN;
        }
        return switch (dataType.toLowerCase(Locale.ROOT)) {
            case "tinyint", "smallint", "mediumint", "int", "integer", "bigint" -> parseIntFamily(dataType);
            case "decimal", "numeric", "number" -> LogicalType.DECIMAL;
            case "float" -> LogicalType.FLOAT;
            case "double", "real", "double precision" -> LogicalType.DOUBLE;
            case "char", "varchar", "text", "longtext", "mediumtext", "tinytext", "nvarchar", "nchar" ->
                    LogicalType.STRING;
            case "date" -> LogicalType.DATE;
            case "time" -> LogicalType.TIME;
            case "datetime", "timestamp" -> LogicalType.TIMESTAMP;
            case "bit", "boolean", "bool" -> LogicalType.BOOLEAN;
            case "blob", "binary", "varbinary", "longblob" -> LogicalType.BINARY;
            default -> LogicalType.UNKNOWN;
        };
    }

    private static LogicalType parseIntFamily(String dataType) {
        return switch (dataType.toLowerCase(Locale.ROOT)) {
            case "bigint" -> LogicalType.LONG;
            default -> LogicalType.INT;
        };
    }

    static LogicalType fromJavaType(String fieldType) {
        if (fieldType == null || fieldType.isBlank()) {
            return LogicalType.UNKNOWN;
        }
        String simple = fieldType.trim();
        int generic = simple.indexOf('<');
        if (generic > 0) {
            simple = simple.substring(0, generic);
        }
        int dot = simple.lastIndexOf('.');
        if (dot >= 0) {
            simple = simple.substring(dot + 1);
        }
        return switch (simple) {
            case "boolean", "Boolean" -> LogicalType.BOOLEAN;
            case "byte", "Byte", "short", "Short", "int", "Integer" -> LogicalType.INT;
            case "long", "Long" -> LogicalType.LONG;
            case "float", "Float" -> LogicalType.FLOAT;
            case "double", "Double" -> LogicalType.DOUBLE;
            case "BigDecimal" -> LogicalType.DECIMAL;
            case "String" -> LogicalType.STRING;
            case "Date", "LocalDate" -> LogicalType.DATE;
            case "Time", "LocalTime" -> LogicalType.TIME;
            case "Timestamp", "LocalDateTime", "Instant", "OffsetDateTime" -> LogicalType.TIMESTAMP;
            case "byte[]" -> LogicalType.BINARY;
            default -> LogicalType.UNKNOWN;
        };
    }
}
