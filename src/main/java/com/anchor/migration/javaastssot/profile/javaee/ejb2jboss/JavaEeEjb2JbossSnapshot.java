package com.anchor.migration.javaastssot.profile.javaee.ejb2jboss;

import com.anchor.migration.javaastssot.profile.ProfileSnapshot;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.CrosswalkEdgeRecord;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.EjbBeanRecord;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.EjbCmpFieldRecord;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.EjbRefRecord;

import java.util.ArrayList;
import java.util.List;

public final class JavaEeEjb2JbossSnapshot implements ProfileSnapshot {
    public final List<EjbBeanRecord> beans = new ArrayList<>();
    public final List<EjbCmpFieldRecord> cmpFields = new ArrayList<>();
    public final List<EjbRefRecord> refs = new ArrayList<>();
    public final List<CrosswalkEdgeRecord> crosswalk = new ArrayList<>();
}
