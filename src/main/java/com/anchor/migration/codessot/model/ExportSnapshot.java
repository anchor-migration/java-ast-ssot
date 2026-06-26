package com.anchor.migration.codessot.model;

import java.util.ArrayList;
import java.util.List;

public final class ExportSnapshot {
    public String sourceRoot;
    public int javaFileCount;
    public final List<SourceFileRecord> sourceFiles = new ArrayList<>();
    public final List<JavaTypeRecord> javaTypes = new ArrayList<>();
    public final List<JavaMethodRecord> javaMethods = new ArrayList<>();
    public final List<JavaFieldRecord> javaFields = new ArrayList<>();
    public final List<JavaImportRecord> javaImports = new ArrayList<>();
    public final List<EjbBeanRecord> ejbBeans = new ArrayList<>();
    public final List<EjbCmpFieldRecord> ejbCmpFields = new ArrayList<>();
    public final List<EjbRefRecord> ejbRefs = new ArrayList<>();
    public final List<CrosswalkEdgeRecord> crosswalkEdges = new ArrayList<>();
}
