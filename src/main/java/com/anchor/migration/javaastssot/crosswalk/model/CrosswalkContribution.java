package com.anchor.migration.javaastssot.crosswalk.model;

import java.util.List;

public record CrosswalkContribution(List<CodeSchemaLinkRecord> links, List<CrosswalkIssue> issues) {}
