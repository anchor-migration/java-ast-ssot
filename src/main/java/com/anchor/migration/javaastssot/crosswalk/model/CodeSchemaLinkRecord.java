package com.anchor.migration.javaastssot.crosswalk.model;

import com.anchor.migration.javaastssot.crosswalk.alignment.LinkAlignment;

public record CodeSchemaLinkRecord(
        String edgeKind,
        String sourceStableId,
        String targetStableId,
        String mappingRole,
        String profileId,
        String bindingSource,
        String evidenceRef,
        String confidence,
        LinkAlignment alignment) {}
