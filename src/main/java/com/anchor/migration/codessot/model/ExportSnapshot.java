package com.anchor.migration.codessot.model;

import com.anchor.migration.codessot.model.profile.JavaEeEjb2JbossSnapshot;
import com.anchor.migration.codessot.profile.javaee.ejb2jboss.JavaEeEjb2JbossProfile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ExportSnapshot {
    public String sourceRoot;
    public int javaFileCount;
    public final Set<String> enabledProfiles = new LinkedHashSet<>();
    public final List<SourceFileRecord> sourceFiles = new ArrayList<>();
    public final List<JavaTypeRecord> javaTypes = new ArrayList<>();
    public final List<JavaMethodRecord> javaMethods = new ArrayList<>();
    public final List<JavaFieldRecord> javaFields = new ArrayList<>();
    public final List<JavaImportRecord> javaImports = new ArrayList<>();

    /** Present when profile {@link JavaEeEjb2JbossProfile#ID} is enabled. */
    public JavaEeEjb2JbossSnapshot javaEeEjb2Jboss;

    public void enableProfile(String profileId) {
        enabledProfiles.add(profileId);
        if (JavaEeEjb2JbossProfile.ID.equals(profileId) && javaEeEjb2Jboss == null) {
            javaEeEjb2Jboss = new JavaEeEjb2JbossSnapshot();
        }
    }
}
