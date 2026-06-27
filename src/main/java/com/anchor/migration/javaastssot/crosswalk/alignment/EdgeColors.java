package com.anchor.migration.javaastssot.crosswalk.alignment;

public final class EdgeColors {
    public static final String GREEN = "green";
    public static final String YELLOW = "yellow";
    public static final String ORANGE = "orange";
    public static final String RED = "red";

    private EdgeColors() {}

    static int severity(String color) {
        return switch (color) {
            case GREEN -> 0;
            case YELLOW -> 1;
            case ORANGE -> 2;
            case RED -> 3;
            default -> 3;
        };
    }

    static String max(String a, String b) {
        return severity(a) >= severity(b) ? a : b;
    }
}
