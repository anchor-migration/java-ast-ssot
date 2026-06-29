package com.anchor.migration.javaastssot.profile.mybatis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MyBatisSqlParser {

    private static final Pattern FROM_CLAUSE =
            Pattern.compile("(?is)\\bFROM\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s+(?:AS\\s+)?([A-Za-z_][A-Za-z0-9_]*))?");
    private static final Pattern JOIN_CLAUSE =
            Pattern.compile("(?is)\\bJOIN\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s+(?:AS\\s+)?([A-Za-z_][A-Za-z0-9_]*))?");
    private static final Pattern QUALIFIED_COLUMN =
            Pattern.compile("(?i)([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)");

    private MyBatisSqlParser() {}

    static String normalize(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }

    static boolean hasJoin(String sql) {
        return sql != null && sql.toUpperCase(Locale.ROOT).contains(" JOIN ");
    }

    static List<String> referencedTables(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        collectClauseTables(sql, FROM_CLAUSE, tables);
        collectClauseTables(sql, JOIN_CLAUSE, tables);
        return new ArrayList<>(tables);
    }

    static Map<String, String> tableAliases(String sql) {
        Map<String, String> aliases = new LinkedHashMap<>();
        collectAliases(sql, FROM_CLAUSE, aliases);
        collectAliases(sql, JOIN_CLAUSE, aliases);
        return aliases;
    }

    static Map<String, String> columnToTable(String sql, List<String> tables, Map<String, String> aliases) {
        Map<String, String> columnTables = new LinkedHashMap<>();
        if (sql == null || sql.isBlank()) {
            return columnTables;
        }
        int selectIdx = sql.toUpperCase(Locale.ROOT).indexOf("SELECT");
        int fromIdx = sql.toUpperCase(Locale.ROOT).indexOf("FROM");
        if (selectIdx < 0 || fromIdx <= selectIdx) {
            return columnTables;
        }
        String selectList = sql.substring(selectIdx + "SELECT".length(), fromIdx);
        Matcher matcher = QUALIFIED_COLUMN.matcher(selectList);
        while (matcher.find()) {
            String alias = matcher.group(1);
            String column = matcher.group(2).toUpperCase(Locale.ROOT);
            String table = aliases.get(alias.toLowerCase(Locale.ROOT));
            if (table != null) {
                columnTables.putIfAbsent(column, table);
            }
        }
        if (tables.size() == 1 && columnTables.isEmpty()) {
            String onlyTable = tables.get(0);
            Matcher bareColumn = Pattern.compile("(?i)\\b([A-Za-z_][A-Za-z0-9_]*)\\b").matcher(selectList);
            while (bareColumn.find()) {
                String token = bareColumn.group(1);
                if (isSqlKeyword(token)) {
                    continue;
                }
                columnTables.putIfAbsent(token.toUpperCase(Locale.ROOT), onlyTable);
            }
        }
        return columnTables;
    }

    private static void collectClauseTables(String sql, Pattern pattern, Set<String> tables) {
        if (sql == null) {
            return;
        }
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            tables.add(matcher.group(1).toUpperCase(Locale.ROOT));
        }
    }

    private static void collectAliases(String sql, Pattern pattern, Map<String, String> aliases) {
        if (sql == null) {
            return;
        }
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).toUpperCase(Locale.ROOT);
            String alias = matcher.group(2);
            if (alias != null && !alias.isBlank()) {
                aliases.put(alias.toLowerCase(Locale.ROOT), table);
            }
            aliases.putIfAbsent(table.toLowerCase(Locale.ROOT), table);
        }
    }

    private static boolean isSqlKeyword(String token) {
        return switch (token.toUpperCase(Locale.ROOT)) {
            case "SELECT", "DISTINCT", "AS", "FROM", "WHERE", "AND", "OR", "ON", "JOIN", "INNER", "LEFT", "RIGHT",
                    "OUTER", "CASE", "WHEN", "THEN", "ELSE", "END" -> true;
            default -> false;
        };
    }
}
