package com.anchor.migration.javaastssot.core.model;

public record JavaTypeRecord(
        String relativePath,
        String stableId,
        String packageName,
        String simpleName,
        String kind,
        String extendsType,
        String implementsList) {}
