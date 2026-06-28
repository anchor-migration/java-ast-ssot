package com.anchor.migration.javaastssot.listusage;

import java.util.List;

public record ListUsageRecord(
        String relativePath,
        String siteStableId,
        SiteKind siteKind,
        String methodStableId,
        String variableName,
        CollectionKind collectionKind,
        UsageClass usageClass,
        List<String> elementTypes,
        Confidence confidence,
        List<ElementEvidence> evidence) {}
