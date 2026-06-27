package com.anchor.migration.javaastssot.core.extract;

import com.anchor.migration.javaastssot.core.model.ExportSnapshot;
import com.anchor.migration.javaastssot.core.model.SourceCommentRecord;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.CompilationUnit;

import java.util.HashSet;
import java.util.Set;

/** Extracts raw comment blocks without binding to semantic AST nodes (ADR-003 sidecar). */
final class SourceCommentExtractor {

    void extract(CompilationUnit cu, String relativePath, ExportSnapshot snapshot) {
        Set<String> seen = new HashSet<>();
        cu.walk(node -> collectComments(node, relativePath, snapshot, seen));
    }

    private void collectComments(
            Node node, String relativePath, ExportSnapshot snapshot, Set<String> seen) {
        node.getComment().ifPresent(comment -> addComment(comment, relativePath, snapshot, seen));
        for (Comment comment : node.getOrphanComments()) {
            addComment(comment, relativePath, snapshot, seen);
        }
    }

    private void addComment(
            Comment comment, String relativePath, ExportSnapshot snapshot, Set<String> seen) {
        if (comment.getRange().isEmpty()) {
            return;
        }
        int startLine = comment.getRange().get().begin.line;
        int endLine = comment.getRange().get().end.line;
        String kind = commentKind(comment);
        String text = comment.getContent().strip();
        if (text.isEmpty()) {
            return;
        }
        String key = relativePath + "|" + startLine + "|" + endLine + "|" + kind + "|" + text;
        if (!seen.add(key)) {
            return;
        }
        snapshot.sourceComments.add(
                new SourceCommentRecord(relativePath, startLine, endLine, kind, text));
    }

    private static String commentKind(Comment comment) {
        if (comment instanceof JavadocComment) {
            return "javadoc";
        }
        if (comment instanceof LineComment) {
            return "line";
        }
        if (comment instanceof BlockComment) {
            return "block";
        }
        return "block";
    }
}
