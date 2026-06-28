package com.anchor.migration.javaastssot.listusage;

import java.time.Instant;
import java.util.List;

public final class ListUsageJsonWriter {

    private ListUsageJsonWriter() {}

    public static String toJson(ListUsageReport report) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\n");
        sb.append("  \"sourceRoot\": ").append(jsonString(report.sourceRoot())).append(",\n");
        sb.append("  \"generatedAt\": ").append(jsonString(Instant.now().toString())).append(",\n");
        sb.append("  \"records\": [\n");
        List<ListUsageRecord> records = report.records();
        for (int i = 0; i < records.size(); i++) {
            writeRecord(sb, records.get(i));
            if (i < records.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append('\n');
            }
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void writeRecord(StringBuilder sb, ListUsageRecord record) {
        sb.append("    {\n");
        sb.append("      \"relativePath\": ").append(jsonString(record.relativePath())).append(",\n");
        sb.append("      \"siteStableId\": ").append(jsonString(record.siteStableId())).append(",\n");
        sb.append("      \"siteKind\": ").append(jsonString(record.siteKind().name())).append(",\n");
        sb.append("      \"methodStableId\": ").append(jsonNullable(record.methodStableId())).append(",\n");
        sb.append("      \"variableName\": ").append(jsonString(record.variableName())).append(",\n");
        sb.append("      \"collectionKind\": ").append(jsonString(record.collectionKind().jsonValue())).append(",\n");
        sb.append("      \"usageClass\": ").append(jsonString(record.usageClass().name())).append(",\n");
        sb.append("      \"elementTypes\": ").append(jsonStringArray(record.elementTypes())).append(",\n");
        sb.append("      \"confidence\": ").append(jsonString(record.confidence().name())).append(",\n");
        sb.append("      \"evidence\": [\n");
        List<ElementEvidence> evidence = record.evidence();
        for (int i = 0; i < evidence.size(); i++) {
            ElementEvidence item = evidence.get(i);
            sb.append("        {\"source\": ")
                    .append(jsonString(item.source()))
                    .append(", \"type\": ")
                    .append(jsonString(item.type()))
                    .append(", \"line\": ")
                    .append(item.line())
                    .append('}');
            if (i < evidence.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append('\n');
            }
        }
        sb.append("      ]\n");
        sb.append("    }");
    }

    private static String jsonNullable(String value) {
        return value == null ? "null" : jsonString(value);
    }

    private static String jsonStringArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(jsonString(values.get(i)));
            if (i < values.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
