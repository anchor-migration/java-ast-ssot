package com.anchor.migration.codessot.profile.javaee.ejb2jboss;

import com.anchor.migration.codessot.StableIds;
import com.anchor.migration.codessot.model.*;
import com.anchor.migration.codessot.model.profile.JavaEeEjb2JbossSnapshot;
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
        JavaEeEjb2JbossSnapshot profile = requireProfile(snapshot);
        String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
        snapshot.sourceFiles.add(new SourceFileRecord(relative, "xml"));

        try (InputStream in = Files.newInputStream(file)) {
            Document doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(in);
            doc.getDocumentElement().normalize();
            parseSessions(doc, relative, profile);
            parseEntities(doc, relative, profile);
        }
    }

    void parseJbossCmpJdbc(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        JavaEeEjb2JbossSnapshot profile = requireProfile(snapshot);
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
                    upsertCmpField(profile, ejbName, fieldName, columnName != null ? columnName : fieldName);
                }
            }

            for (int i = 0; i < profile.ejbBeans.size(); i++) {
                EjbBeanRecord bean = profile.ejbBeans.get(i);
                String table = ejbToTable.get(bean.ejbName());
                if (table == null) {
                    continue;
                }
                profile.ejbBeans.set(
                        i,
                        new EjbBeanRecord(
                                bean.descriptorFile(),
                                bean.ejbName(),
                                bean.ejbClass(),
                                bean.beanType(),
                                bean.sessionType(),
                                bean.persistenceType(),
                                table));
                profile.crosswalkEdges.add(
                        new CrosswalkEdgeRecord(
                                "ejb_to_table",
                                StableIds.ejbBean(bean.ejbName()),
                                StableIds.dbTable(JavaEeEjb2JbossProfile.DEFAULT_DB_SCHEMA, table)));
                if (bean.ejbClass() != null) {
                    profile.crosswalkEdges.add(
                            new CrosswalkEdgeRecord(
                                    "java_type_to_ejb",
                                    bean.ejbClass(),
                                    StableIds.ejbBean(bean.ejbName())));
                }
            }
        }
    }

    private void parseSessions(Document doc, String descriptorFile, JavaEeEjb2JbossSnapshot profile) {
        NodeList sessions = doc.getElementsByTagName("session");
        for (int i = 0; i < sessions.getLength(); i++) {
            Element session = (Element) sessions.item(i);
            String ejbName = text(session, "ejb-name");
            String ejbClass = text(session, "ejb-class");
            String sessionType = text(session, "session-type");
            profile.ejbBeans.add(
                    new EjbBeanRecord(
                            descriptorFile, ejbName, ejbClass, "session", sessionType, null, null));
            parseRefs(session, ejbName, profile);
        }
    }

    private void parseEntities(Document doc, String descriptorFile, JavaEeEjb2JbossSnapshot profile) {
        NodeList entities = doc.getElementsByTagName("entity");
        for (int i = 0; i < entities.getLength(); i++) {
            Element entity = (Element) entities.item(i);
            String ejbName = text(entity, "ejb-name");
            String ejbClass = text(entity, "ejb-class");
            String persistenceType = text(entity, "persistence-type");
            profile.ejbBeans.add(
                    new EjbBeanRecord(
                            descriptorFile, ejbName, ejbClass, "entity", null, persistenceType, null));
            NodeList cmpFields = entity.getElementsByTagName("cmp-field");
            for (int j = 0; j < cmpFields.getLength(); j++) {
                Element cmp = (Element) cmpFields.item(j);
                String fieldName = text(cmp, "field-name");
                if (fieldName != null) {
                    profile.ejbCmpFields.add(new EjbCmpFieldRecord(ejbName, fieldName, null));
                }
            }
            parseRefs(entity, ejbName, profile);
        }
    }

    private void parseRefs(Element parent, String sourceEjb, JavaEeEjb2JbossSnapshot profile) {
        addRefs(parent, sourceEjb, profile, "ejb-local-ref", "ejb-local-ref");
        addRefs(parent, sourceEjb, profile, "ejb-ref", "ejb-ref");
        addRefs(parent, sourceEjb, profile, "resource-ref", "resource-ref");
    }

    private void addRefs(
            Element parent, String sourceEjb, JavaEeEjb2JbossSnapshot profile, String tag, String kind) {
        NodeList refs = parent.getElementsByTagName(tag);
        for (int i = 0; i < refs.getLength(); i++) {
            Element ref = (Element) refs.item(i);
            String refName =
                    text(ref, tag.equals("resource-ref") ? "res-ref-name" : "ejb-ref-name");
            String refType = text(ref, tag.equals("resource-ref") ? "res-type" : "ejb-ref-type");
            profile.ejbRefs.add(
                    new EjbRefRecord(sourceEjb, refName, refType, text(ref, "ejb-link"), kind));
        }
    }

    private void upsertCmpField(
            JavaEeEjb2JbossSnapshot profile, String ejbName, String fieldName, String columnName) {
        for (int i = 0; i < profile.ejbCmpFields.size(); i++) {
            EjbCmpFieldRecord existing = profile.ejbCmpFields.get(i);
            if (existing.ejbName().equals(ejbName) && existing.fieldName().equals(fieldName)) {
                profile.ejbCmpFields.set(i, new EjbCmpFieldRecord(ejbName, fieldName, columnName));
                return;
            }
        }
        profile.ejbCmpFields.add(new EjbCmpFieldRecord(ejbName, fieldName, columnName));
    }

    private JavaEeEjb2JbossSnapshot requireProfile(ExportSnapshot snapshot) {
        if (snapshot.javaEeEjb2Jboss == null) {
            throw new IllegalStateException("Profile " + JavaEeEjb2JbossProfile.ID + " is not enabled");
        }
        return snapshot.javaEeEjb2Jboss;
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
