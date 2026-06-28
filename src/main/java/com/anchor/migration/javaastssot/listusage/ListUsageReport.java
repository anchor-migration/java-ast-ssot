package com.anchor.migration.javaastssot.listusage;

import java.util.ArrayList;
import java.util.List;

public record ListUsageReport(String sourceRoot, List<ListUsageRecord> records) {

    public static ListUsageReport empty(String sourceRoot) {
        return new ListUsageReport(sourceRoot, List.of());
    }

    public ListUsageReport withRecords(List<ListUsageRecord> merged) {
        return new ListUsageReport(sourceRoot, List.copyOf(merged));
    }
}
