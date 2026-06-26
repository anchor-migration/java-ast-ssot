package com.anchor.migration.codessot.model.profile;

import com.anchor.migration.codessot.model.CrosswalkEdgeRecord;
import com.anchor.migration.codessot.model.EjbBeanRecord;
import com.anchor.migration.codessot.model.EjbCmpFieldRecord;
import com.anchor.migration.codessot.model.EjbRefRecord;

import java.util.ArrayList;
import java.util.List;

/** Snapshot data for profile {@code javaee-ejb2-jboss}. */
public final class JavaEeEjb2JbossSnapshot {
    public final List<EjbBeanRecord> ejbBeans = new ArrayList<>();
    public final List<EjbCmpFieldRecord> ejbCmpFields = new ArrayList<>();
    public final List<EjbRefRecord> ejbRefs = new ArrayList<>();
    public final List<CrosswalkEdgeRecord> crosswalkEdges = new ArrayList<>();
}
