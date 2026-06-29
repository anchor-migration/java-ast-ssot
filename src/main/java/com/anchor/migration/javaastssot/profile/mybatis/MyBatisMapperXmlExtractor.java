package com.anchor.migration.javaastssot.profile.mybatis;

import com.anchor.migration.javaastssot.core.model.ExportSnapshot;
import com.anchor.migration.javaastssot.core.model.SourceFileRecord;
import com.anchor.migration.javaastssot.crosswalk.MappingRoles;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisResultFieldRecord;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisResultMapRecord;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisStatementRecord;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisStatementTableRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MyBatisMapperXmlExtractor {

    void parseFile(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
        MyBatisSnapshot data = snapshot.requireProfile(MyBatisProfile.ID, MyBatisSnapshot.class);
        addSourceFile(snapshot, relative);

        try (InputStream in = Files.newInputStream(file)) {
            Document doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(in);
            doc.getDocumentElement().normalize();
            Element mapper = doc.getDocumentElement();
            if (!"mapper".equals(mapper.getTagName())) {
                return;
            }

            Map<String, ResultMapBuilder> resultMaps = parseResultMaps(relative, mapper);
            List<StatementBuilder> statements = parseStatements(relative, mapper);
            linkStatements(relative, data, resultMaps, statements);
        }
    }

    boolean looksLikeMapper(Path file) {
        if (!file.getFileName().toString().endsWith(".xml")) {
            return false;
        }
        try {
            String content = Files.readString(file);
            return content.contains("<mapper") && content.contains("namespace=");
        } catch (Exception ex) {
            return false;
        }
    }

    private void linkStatements(
            String relative,
            MyBatisSnapshot data,
            Map<String, ResultMapBuilder> resultMaps,
            List<StatementBuilder> statements) {
        for (StatementBuilder statement : statements) {
            String sql = MyBatisSqlParser.normalize(statement.sqlText);
            boolean joinQuery = MyBatisSqlParser.hasJoin(sql);
            List<String> tables = MyBatisSqlParser.referencedTables(sql);
            Map<String, String> aliases = MyBatisSqlParser.tableAliases(sql);
            Map<String, String> columnTables = MyBatisSqlParser.columnToTable(sql, tables, aliases);

            String mappingRole = null;
            if (joinQuery) {
                mappingRole = MappingRoles.READ_MODEL;
                if (statement.resultMapId != null) {
                    ResultMapBuilder resultMap = resultMaps.get(statement.resultMapId);
                    if (resultMap != null) {
                        resultMap.mappingRole = MappingRoles.READ_MODEL;
                        resultMap.statementId = statement.id;
                        resultMap.tableName = null;
                        applyColumnTables(resultMap, columnTables);
                    }
                }
            } else if (statement.resultMapId != null && tables.size() == 1) {
                ResultMapBuilder resultMap = resultMaps.get(statement.resultMapId);
                if (resultMap != null) {
                    resultMap.mappingRole = MappingRoles.PERSISTENT_ENTITY;
                    resultMap.tableName = tables.get(0);
                    resultMap.statementId = statement.id;
                    for (FieldBuilder field : resultMap.fields) {
                        field.tableName = null;
                    }
                }
            }

            data.statements.add(
                    new MyBatisStatementRecord(
                            relative,
                            statement.id,
                            statement.type,
                            statement.resultMapId,
                            statement.resultTypeStableId,
                            sql,
                            joinQuery,
                            mappingRole));

            for (String table : tables) {
                data.statementTables.add(new MyBatisStatementTableRecord(relative, statement.id, table));
            }
        }

        for (ResultMapBuilder builder : resultMaps.values()) {
            data.resultMaps.add(
                    new MyBatisResultMapRecord(
                            relative,
                            builder.id,
                            builder.typeStableId,
                            builder.mappingRole,
                            builder.tableName,
                            builder.statementId));
            for (FieldBuilder field : builder.fields) {
                data.resultFields.add(
                        new MyBatisResultFieldRecord(
                                relative, builder.id, field.property, field.column, field.tableName));
            }
        }
    }

    private static void applyColumnTables(ResultMapBuilder resultMap, Map<String, String> columnTables) {
        for (FieldBuilder field : resultMap.fields) {
            String table = columnTables.get(field.column.toUpperCase(Locale.ROOT));
            if (table != null) {
                field.tableName = table;
            }
        }
    }

    private Map<String, ResultMapBuilder> parseResultMaps(String relative, Element mapper) {
        Map<String, ResultMapBuilder> resultMaps = new HashMap<>();
        NodeList nodes = mapper.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element element) || !"resultMap".equals(element.getTagName())) {
                continue;
            }
            String id = element.getAttribute("id");
            String type = element.getAttribute("type");
            if (id.isBlank() || type.isBlank()) {
                continue;
            }
            ResultMapBuilder builder = new ResultMapBuilder(id, type);
            NodeList children = element.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (!(child instanceof Element mapping)) {
                    continue;
                }
                String tag = mapping.getTagName();
                if (!"id".equals(tag) && !"result".equals(tag)) {
                    continue;
                }
                String property = mapping.getAttribute("property");
                String column = mapping.getAttribute("column");
                if (!property.isBlank() && !column.isBlank()) {
                    builder.fields.add(new FieldBuilder(property, column));
                }
            }
            resultMaps.put(id, builder);
        }
        return resultMaps;
    }

    private List<StatementBuilder> parseStatements(String relative, Element mapper) {
        List<StatementBuilder> statements = new ArrayList<>();
        NodeList nodes = mapper.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element element)) {
                continue;
            }
            String tag = element.getTagName();
            if (!isStatementTag(tag)) {
                continue;
            }
            String id = element.getAttribute("id");
            if (id.isBlank()) {
                continue;
            }
            StatementBuilder statement = new StatementBuilder(id, tag);
            statement.resultMapId = emptyToNull(element.getAttribute("resultMap"));
            statement.resultTypeStableId = emptyToNull(element.getAttribute("resultType"));
            statement.sqlText = elementText(element);
            statements.add(statement);
        }
        return statements;
    }

    private static boolean isStatementTag(String tag) {
        return "select".equals(tag) || "insert".equals(tag) || "update".equals(tag) || "delete".equals(tag);
    }

    private static String elementText(Element element) {
        StringBuilder sb = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(child.getNodeValue());
            }
        }
        return sb.toString();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void addSourceFile(ExportSnapshot snapshot, String relative) {
        if (snapshot.sourceFiles.stream().noneMatch(s -> s.relativePath().equals(relative))) {
            snapshot.sourceFiles.add(new SourceFileRecord(relative, "xml"));
        }
    }

    private static final class ResultMapBuilder {
        final String id;
        final String typeStableId;
        String mappingRole = MappingRoles.PERSISTENT_ENTITY;
        String tableName;
        String statementId;
        final List<FieldBuilder> fields = new ArrayList<>();

        ResultMapBuilder(String id, String type) {
            this.id = id;
            this.typeStableId = type;
        }
    }

    private static final class FieldBuilder {
        final String property;
        final String column;
        String tableName;

        FieldBuilder(String property, String column) {
            this.property = property;
            this.column = column;
        }
    }

    private static final class StatementBuilder {
        final String id;
        final String type;
        String resultMapId;
        String resultTypeStableId;
        String sqlText;

        StatementBuilder(String id, String type) {
            this.id = id;
            this.type = type;
        }
    }
}
