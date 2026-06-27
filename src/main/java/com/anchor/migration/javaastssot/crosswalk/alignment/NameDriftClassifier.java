package com.anchor.migration.javaastssot.crosswalk.alignment;

import java.util.Map;

final class NameDriftClassifier {

    private static final Map<String, String> EXPLAINABLE_ALIASES =
            Map.of("acct", "account", "cust", "customer", "qty", "quantity", "num", "number");

    private NameDriftClassifier() {}

    static String classify(String sourceName, String targetName, boolean typeLabel) {
        String normalizedSource =
                typeLabel ? NameNormalizer.normalizeTypeLabel(sourceName) : NameNormalizer.normalizeIdentifier(sourceName);
        String normalizedTarget =
                typeLabel ? NameNormalizer.normalizeTypeLabel(targetName) : NameNormalizer.normalizeIdentifier(targetName);

        if (normalizedSource.equals(normalizedTarget)) {
            return NameDriftClasses.NONE;
        }
        if (isExplainablePair(normalizedSource, normalizedTarget)) {
            return NameDriftClasses.EXPLAINABLE;
        }
        if (sharesMeaningfulOverlap(normalizedSource, normalizedTarget)) {
            return NameDriftClasses.QUESTIONABLE;
        }
        return NameDriftClasses.UNEXPLAINABLE;
    }

    private static boolean isExplainablePair(String a, String b) {
        if (EXPLAINABLE_ALIASES.getOrDefault(a, a).equals(EXPLAINABLE_ALIASES.getOrDefault(b, b))) {
            return !a.equals(b);
        }
        return EXPLAINABLE_ALIASES.containsKey(a) && EXPLAINABLE_ALIASES.get(a).equals(b)
                || EXPLAINABLE_ALIASES.containsKey(b) && EXPLAINABLE_ALIASES.get(b).equals(a);
    }

    private static boolean sharesMeaningfulOverlap(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (a.contains(b) || b.contains(a)) {
            return true;
        }
        int prefix = commonPrefixLength(a, b);
        return prefix >= 3;
    }

    private static int commonPrefixLength(String a, String b) {
        int limit = Math.min(a.length(), b.length());
        int i = 0;
        while (i < limit && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }
}
