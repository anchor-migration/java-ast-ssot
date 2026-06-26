package com.anchor.migration.javaastssot.profile.javaee.ejb2jboss;

import com.anchor.migration.javaastssot.core.model.ExportSnapshot;
import com.anchor.migration.javaastssot.core.model.SourceFileRecord;
import com.anchor.migration.javaastssot.profile.ProfileRegistry;
import com.anchor.migration.javaastssot.profile.ProfileSnapshot;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.CrosswalkEdgeRecord;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.EjbBeanRecord;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.EjbCmpFieldRecord;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.EjbRefRecord;
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

final class EjbDescriptorExtractor {

    void parseEjbJar(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        JavaEeEjb2JbossSnapshot data = snapshot.requireProfile(ID, JavaEeEjb2JbossSnapshot.class);
        String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
        addSourceFile(snapshot, relative);

        try (InputStream in = Files.newInputStream(file)) {
            Document doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(in);
            doc.getDocumentElement().normalize();
            parseSessions(doc, relative, data);
            parseEntities(doc, relative, data);
        }
    }

    void parseJbossCmpJdbc(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        JavaEeEjb2JbossSnapshot data = snapshot.requireProfile(ID, JavaEeEjb2JbossSnapshot.class);
        String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
        addSourceFile(snapshot, relative);

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
                    upsertCmpField(data, ejbName, fieldName, columnName != null ? columnName : fieldName);
                }
            }

            for (int i = 0; i < data.beans.size(); i++) {
                EjbBeanRecord bean = data.beans.get(i);
                String table = ejbToTable.get(bean.ejbName());
                if (table == null) {
                    continue;
                }
                data.beans.set(
                        i,
                        new EjbBeanRecord(
                                bean.descriptorFile(),
                                bean.ejbName(),
                                bean.ejbClass(),
                                bean.beanType(),
                                bean.sessionType(),
                                bean.persistenceType(),
                                table));
                data.crosswalk.add(
                        new CrosswalkEdgeRecord(
                                "ejb_to_table",
                                JavaEeEjb2JbossIds.ejbBean(bean.ejbName()),
                                JavaEeEjb2JbossIds.dbTable(JavaEeEjb2JbossIds.DEFAULT_DB_SCHEMA, table)));
                if (bean.ejbClass() != null) {
                    data.crosswalk.add(
                            new CrosswalkEdgeRecord(
                                    "java_type_to_ejb", bean.ejbClass(), JavaEeEjb2JbossIds.ejbBean(bean.ejbName())));
                }
            }
        }
    }

    private static final String ID = JavaEeEjb2JbossProfile.ID;

    private void addSourceFile(ExportSnapshot snapshot, String relative) {
        if (snapshot.sourceFiles.stream().noneMatch(s -> s.relativePath().equals(relative))) {
            snapshot.sourceFiles.add(new SourceFileRecord(relative, "xml"));
        }
    }

    private void parseSessions(Document doc, String descriptorFile, JavaEeEjb2JbossSnapshot data) {
        NodeList sessions = doc.getElementsByTagName("session");
        for (int i = 0; i < sessions.getLength(); i++) {
            Element session = (Element) sessions.item(i);
            String ejbName = text(session, "ejb-name");
            String ejbClass = text(session, "ejb-class");
            String sessionType = text(session, "session-type");
            data.beans.add(
                    new EjbBeanRecord(
                            descriptorFile, ejbName, ejbClass, "session", sessionType, null, null));
            parseRefs(session, ejbName, data);
        }
    }

    private void parseEntities(Document doc, String descriptorFile, JavaEeEjb2JbossSnapshot data) {
        NodeList entities = doc.getElementsByTagName("entity");
        for (int i = 0; i < entities.getLength(); i++) {
            Element entity = (Element) entities.item(i);
            String ejbName = text(entity, "ejb-name");
            String ejbClass = text(entity, "ejb-class");
            String persistenceType = text(entity, "persistence-type");
            data.beans.add(
                    new EjbBeanRecord(
                            descriptorFile, ejbName, ejbClass, "entity", null, persistenceType, null));
            NodeList cmpFields = entity.getElementsByTagName("cmp-field");
            for (int j = 0; j < cmpFields.getLength(); j++) {
                Element cmp = (Element) cmpFields.item(j);
                String fieldName = text(cmp, "field-name");
                if (fieldName != null) {
                    data.cmpFields.add(new EjbCmpFieldRecord(ejbName, fieldName, null));
                }
            }
            parseRefs(entity, ejbName, data);
        }
    }

    private void parseRefs(Element parent, String sourceEjb, JavaEeEjb2JbossSnapshot data) {
        addRefs(parent, sourceEjb, data, "ejb-local-ref", "ejb-local-ref");
        addRefs(parent, sourceEjb, data, "ejb-ref", "ejb-ref");
        addRefs(parent, sourceEjb, data, "resource-ref", "resource-ref");
    }

    private void addRefs(
            Element parent, String sourceEjb, JavaEeEjb2JbossSnapshot data, String tag, String kind) {
        NodeList refs = parent.getElementsByTagName(tag);
        for (int i = 0; i < refs.getLength(); i++) {
            Element ref = (Element) refs.item(i);
            String refName =
                    text(ref, tag.equals("resource-ref") ? "res-ref-name" : "ejb-ref-name");
            String refType = text(ref, tag.equals("resource-ref") ? "res-type" : "ejb-ref-type");
            data.refs.add(
                    new EjbRefRecord(sourceEjb, refName, refType, text(ref, "ejb-link"), kind));
        }
    }

    private void upsertCmpField(
            JavaEeEjb2JbossSnapshot data, String ejbName, String fieldName, String columnName) {
        for (int i = 0; i < data.cmpFields.size(); i++) {
            EjbCmpFieldRecord existing = data.cmpFields.get(i);
            if (existing.ejbName().equals(ejbName) && existing.fieldName().equals(fieldName)) {
                data.cmpFields.set(i, new EjbCmpFieldRecord(ejbName, fieldName, columnName));
                return;
            }
        }
        data.cmpFields.add(new EjbCmpFieldRecord(ejbName, fieldName, columnName));
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
