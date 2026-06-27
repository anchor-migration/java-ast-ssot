package com.anchor.migration.javaastssot.crosswalk.model;

public record CodeSchemaLinkRecord(
        String edgeKind,
        String sourceStableId,
        String targetStableId,
        String mappingRole,
        String profileId,
        String bindingSource,
        String evidenceRef,
        String confidence) {}
