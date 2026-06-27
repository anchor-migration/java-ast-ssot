package com.anchor.migration.javaastssot.crosswalk.contributor;

import com.anchor.migration.javaastssot.crosswalk.contributor.JavaEeEjb2JbossCrosswalkContributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CrosswalkContributorRegistry {

    private static final Map<String, CrosswalkContributor> BY_PROFILE = new LinkedHashMap<>();

    static {
        register(new JavaEeEjb2JbossCrosswalkContributor());
    }

    private CrosswalkContributorRegistry() {}

    public static void register(CrosswalkContributor contributor) {
        BY_PROFILE.put(contributor.profileId(), contributor);
    }

    public static CrosswalkContributor require(String profileId) {
        CrosswalkContributor contributor = BY_PROFILE.get(profileId);
        if (contributor == null) {
            throw new IllegalArgumentException("No crosswalk contributor for profile: " + profileId);
        }
        return contributor;
    }

    public static List<CrosswalkContributor> forProfiles(List<String> profileIds) {
        return profileIds.stream().map(CrosswalkContributorRegistry::require).toList();
    }
}
