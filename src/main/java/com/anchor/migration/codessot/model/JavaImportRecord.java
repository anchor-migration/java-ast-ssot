package com.anchor.migration.codessot.model;

public record JavaImportRecord(
        String relativePath, String importedName, boolean isStatic, boolean isWildcard) {}
