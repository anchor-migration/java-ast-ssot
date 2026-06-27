package com.anchor.migration.javaastssot.core.model;

import com.anchor.migration.javaastssot.profile.ProfileSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** In-memory export result: Java core + optional profile snapshots. */
public final class ExportSnapshot {
    public String sourceRoot;
    public int javaFileCount;
    public final Set<String> enabledProfileIds = new LinkedHashSet<>();
    public final List<SourceFileRecord> sourceFiles = new ArrayList<>();
    public final List<JavaTypeRecord> javaTypes = new ArrayList<>();
    public final List<JavaMethodRecord> javaMethods = new ArrayList<>();
    public final List<JavaFieldRecord> javaFields = new ArrayList<>();
    public final List<JavaImportRecord> javaImports = new ArrayList<>();
    public final List<SourceCommentRecord> sourceComments = new ArrayList<>();

    private final Map<String, ProfileSnapshot> profiles = new LinkedHashMap<>();

    public void attachProfile(String profileId, ProfileSnapshot snapshot) {
        enabledProfileIds.add(profileId);
        profiles.put(profileId, snapshot);
    }

    public ProfileSnapshot profile(String profileId) {
        return profiles.get(profileId);
    }

    public <T extends ProfileSnapshot> T requireProfile(String profileId, Class<T> type) {
        T snapshot = type.cast(profiles.get(profileId));
        if (snapshot == null) {
            throw new IllegalStateException("Profile not enabled: " + profileId);
        }
        return snapshot;
    }

    public boolean hasProfile(String profileId) {
        return profiles.containsKey(profileId);
    }
}
