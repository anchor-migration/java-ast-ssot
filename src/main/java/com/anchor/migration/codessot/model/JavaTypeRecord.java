package com.anchor.migration.codessot.model;

public record JavaTypeRecord(
        String relativePath,
        String stableId,
        String packageName,
        String simpleName,
        String kind,
        String extendsType,
        String implementsList) {}
