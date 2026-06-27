package com.anchor.migration.javaastssot.crosswalk.alignment;

import java.util.Locale;
import java.util.Set;

final class NameNormalizer {

    private static final Set<String> TYPE_SUFFIXES = Set.of("bean", "entity", "dto", "vo", "record");

    private NameNormalizer() {}

    static String normalizeIdentifier(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    static String normalizeTypeLabel(String name) {
        String normalized = normalizeIdentifier(name);
        for (String suffix : TYPE_SUFFIXES) {
            if (normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
                normalized = normalized.substring(0, normalized.length() - suffix.length());
            }
        }
        return normalized;
    }
}
