package com.anchor.migration.javaastssot.core.model;

public record SourceCommentRecord(
        String relativePath, int startLine, int endLine, String kind, String text) {}
