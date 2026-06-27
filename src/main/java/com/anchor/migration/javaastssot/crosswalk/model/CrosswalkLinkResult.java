package com.anchor.migration.javaastssot.crosswalk.model;

import java.util.List;

public record CrosswalkLinkResult(
        int crosswalkRunId,
        int linkCount,
        int issueCount,
        int errorCount,
        List<String> profilesLinked) {}
