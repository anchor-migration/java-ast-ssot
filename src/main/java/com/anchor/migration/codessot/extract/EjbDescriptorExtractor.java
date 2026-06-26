package com.anchor.migration.codessot.extract;

import com.anchor.migration.codessot.StableIds;
import com.anchor.migration.codessot.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class EjbDescriptorExtractor {

    public void parseEjbJar(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
        snapshot.sourceFiles.add(new SourceFileRecord(relative, "xml"));

        try (InputStream in = Files.newInputStream(file)) {
            Document doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(in);
            doc.getDocumentElement().normalize();
            parseSessions(doc, relative, snapshot);
            parseEntities(doc, relative, snapshot);
        }
    }

    public void parseJbossCmpJdbc(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
        if (snapshot.sourceFiles.stream().noneMatch(s -> s.relativePath().equals(relative))) {
            snapshot.sourceFiles.add(new SourceFileRecord(relative, "xml"));
        }

        try (InputStream in = Files.newInputStream(file)) {
            Document doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(in);
            doc.getDocumentElement().normalize();

            Map<String, String> ejbToTable = new HashMap<>();
            NodeList entities = doc.getElementsByTagName("entity");
            for (int i = 0; i < entities.getLength(); i++) {
                Element entity = (Element) entities.item(i);
                String ejbName = text(entity, "ejb-name");
                String tableName = text(entity, "table-name");
                if (ejbName != null && tableName != null) {
                    ejbToTable.put(ejbName, tableName);
                }
                NodeList cmpFields = entity.getElementsByTagName("cmp-field");
                for (int j = 0; j < cmpFields.getLength(); j++) {
                    Element cmp = (Element) cmpFields.item(j);
                    String fieldName = text(cmp, "field-name");
                    String columnName = text(cmp, "column-name");
                    if (fieldName == null) {
                        continue;
                    }
                    upsertCmpField(snapshot, ejbName, fieldName, columnName != null ? columnName : fieldName);
                }
            }

            for (int i = 0; i < snapshot.ejbBeans.size(); i++) {
                EjbBeanRecord bean = snapshot.ejbBeans.get(i);
                String table = ejbToTable.get(bean.ejbName());
                if (table == null) {
                    continue;
                }
                snapshot.ejbBeans.set(
                        i,
                        new EjbBeanRecord(
                                bean.descriptorFile(),
                                bean.ejbName(),
                                bean.ejbClass(),
                                bean.beanType(),
                                bean.sessionType(),
                                bean.persistenceType(),
                                table));
                snapshot.crosswalkEdges.add(
                        new CrosswalkEdgeRecord(
                                "ejb_to_table",
                                StableIds.ejbBean(bean.ejbName()),
                                StableIds.dbTable("dukesbank", table)));
                if (bean.ejbClass() != null) {
                    snapshot.crosswalkEdges.add(
                            new CrosswalkEdgeRecord(
                                    "java_type_to_ejb",
                                    bean.ejbClass(),
                                    StableIds.ejbBean(bean.ejbName())));
                }
            }
        }
    }

    private void parseSessions(Document doc, String descriptorFile, ExportSnapshot snapshot) {
        NodeList sessions = doc.getElementsByTagName("session");
        for (int i = 0; i < sessions.getLength(); i++) {
            Element session = (Element) sessions.item(i);
            String ejbName = text(session, "ejb-name");
            String ejbClass = text(session, "ejb-class");
            String sessionType = text(session, "session-type");
            snapshot.ejbBeans.add(
                    new EjbBeanRecord(
                            descriptorFile, ejbName, ejbClass, "session", sessionType, null, null));
            parseRefs(session, ejbName, snapshot);
        }
    }

    private void parseEntities(Document doc, String descriptorFile, ExportSnapshot snapshot) {
        NodeList entities = doc.getElementsByTagName("entity");
        for (int i = 0; i < entities.getLength(); i++) {
            Element entity = (Element) entities.item(i);
            String ejbName = text(entity, "ejb-name");
            String ejbClass = text(entity, "ejb-class");
            String persistenceType = text(entity, "persistence-type");
            snapshot.ejbBeans.add(
                    new EjbBeanRecord(
                            descriptorFile, ejbName, ejbClass, "entity", null, persistenceType, null));
            NodeList cmpFields = entity.getElementsByTagName("cmp-field");
            for (int j = 0; j < cmpFields.getLength(); j++) {
                Element cmp = (Element) cmpFields.item(j);
                String fieldName = text(cmp, "field-name");
                if (fieldName != null) {
                    snapshot.ejbCmpFields.add(new EjbCmpFieldRecord(ejbName, fieldName, null));
                }
            }
            parseRefs(entity, ejbName, snapshot);
        }
    }

    private void parseRefs(Element parent, String sourceEjb, ExportSnapshot snapshot) {
        addRefs(parent, sourceEjb, snapshot, "ejb-local-ref", "ejb-local-ref");
        addRefs(parent, sourceEjb, snapshot, "ejb-ref", "ejb-ref");
        addRefs(parent, sourceEjb, snapshot, "resource-ref", "resource-ref");
    }

    private void addRefs(
            Element parent, String sourceEjb, ExportSnapshot snapshot, String tag, String kind) {
        NodeList refs = parent.getElementsByTagName(tag);
        for (int i = 0; i < refs.getLength(); i++) {
            Element ref = (Element) refs.item(i);
            String refName =
                    text(ref, tag.equals("resource-ref") ? "res-ref-name" : "ejb-ref-name");
            String refType = text(ref, tag.equals("resource-ref") ? "res-type" : "ejb-ref-type");
            snapshot.ejbRefs.add(
                    new EjbRefRecord(sourceEjb, refName, refType, text(ref, "ejb-link"), kind));
        }
    }

    private void upsertCmpField(
            ExportSnapshot snapshot, String ejbName, String fieldName, String columnName) {
        for (int i = 0; i < snapshot.ejbCmpFields.size(); i++) {
            EjbCmpFieldRecord existing = snapshot.ejbCmpFields.get(i);
            if (existing.ejbName().equals(ejbName) && existing.fieldName().equals(fieldName)) {
                snapshot.ejbCmpFields.set(i, new EjbCmpFieldRecord(ejbName, fieldName, columnName));
                return;
            }
        }
        snapshot.ejbCmpFields.add(new EjbCmpFieldRecord(ejbName, fieldName, columnName));
    }

    private String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node.getTextContent() != null ? node.getTextContent().trim() : null;
    }
}
