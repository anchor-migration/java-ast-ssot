package com.anchor.migration.javaastssot.core;

public final class StableIds {
    private StableIds() {}

    public static String javaType(String packageName, String simpleName) {
        if (packageName == null || packageName.isBlank()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }

    public static String javaMethod(String typeId, String name, String parameterTypes) {
        return typeId + "#" + name + "(" + parameterTypes + ")";
    }

    public static String javaField(String typeId, String name) {
        return typeId + "#" + name;
    }
}
