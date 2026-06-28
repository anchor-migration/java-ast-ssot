package com.anchor.migration.javaastssot.listusage;

import java.util.HashMap;
import java.util.Map;

public final class TypeNameNormalizer {

    private static final Map<String, String> PRIMITIVE_TO_BOX = Map.of(
            "int", "Integer",
            "long", "Long",
            "boolean", "Boolean",
            "double", "Double",
            "float", "Float",
            "short", "Short",
            "byte", "Byte",
            "char", "Character");

    private TypeNameNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank() || "unknown".equals(raw)) {
            return "unknown";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("java.lang.")) {
            trimmed = trimmed.substring("java.lang.".length());
        }
        return PRIMITIVE_TO_BOX.getOrDefault(trimmed, trimmed);
    }

    public static boolean areCompatible(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if ("unknown".equals(na) || "unknown".equals(nb)) {
            return true;
        }
        return na.equals(nb);
    }

    public static UsageClass classifyTypes(Iterable<String> rawTypes) {
        String anchor = null;
        for (String raw : rawTypes) {
            String normalized = normalize(raw);
            if ("unknown".equals(normalized)) {
                continue;
            }
            if (anchor == null) {
                anchor = normalized;
            } else if (!areCompatible(anchor, normalized)) {
                return UsageClass.tuple;
            }
        }
        return anchor == null ? UsageClass.unknown : UsageClass.homogeneous;
    }
}
