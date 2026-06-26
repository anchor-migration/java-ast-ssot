package com.anchor.migration.javaastssot.core.model;

public record JavaImportRecord(
        String relativePath, String importedName, boolean isStatic, boolean isWildcard) {}
