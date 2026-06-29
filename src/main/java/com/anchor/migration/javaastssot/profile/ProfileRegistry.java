package com.anchor.migration.javaastssot.profile;

import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.JavaEeEjb2JbossProfile;
import com.anchor.migration.javaastssot.profile.jpa.JpaProfile;
import com.anchor.migration.javaastssot.profile.mybatis.MyBatisProfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProfileRegistry {

    private static final Map<String, ExportProfile> PROFILES = new LinkedHashMap<>();

    static {
        PROFILES.put(JavaEeEjb2JbossProfile.ID, new JavaEeEjb2JbossProfile());
        PROFILES.put(JpaProfile.ID, new JpaProfile());
        PROFILES.put(MyBatisProfile.ID, new MyBatisProfile());
    }

    private ProfileRegistry() {}

    public static Collection<String> knownProfileIds() {
        return PROFILES.keySet();
    }

    public static ExportProfile require(String profileId) {
        ExportProfile profile = PROFILES.get(profileId);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown profile '" + profileId + "'. Known: " + PROFILES.keySet());
        }
        return profile;
    }

    public static Set<String> resolve(List<String> explicitProfiles, Path sourceRoot, boolean autoDetect)
            throws IOException {
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        for (String raw : explicitProfiles) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            for (String part : raw.split(",")) {
                String id = part.trim();
                if (!id.isEmpty()) {
                    require(id);
                    resolved.add(id);
                }
            }
        }
        if (autoDetect) {
            for (ExportProfile profile : PROFILES.values()) {
                if (profile.detect(sourceRoot)) {
                    resolved.add(profile.id());
                }
            }
        }
        return resolved;
    }

    public static List<ExportProfile> orderedProfiles(Set<String> profileIds) {
        List<ExportProfile> active = new ArrayList<>();
        for (String id : profileIds) {
            active.add(require(id));
        }
        return active;
    }

    public static boolean containsHandledFile(Path sourceRoot, ExportProfile profile) throws IOException {
        try (var walk = Files.walk(sourceRoot)) {
            return walk.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .anyMatch(profile::handlesFileName);
        }
    }
}
