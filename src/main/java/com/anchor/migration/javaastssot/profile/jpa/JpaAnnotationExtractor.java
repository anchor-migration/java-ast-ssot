package com.anchor.migration.javaastssot.profile.jpa;

import com.anchor.migration.javaastssot.core.StableIds;
import com.anchor.migration.javaastssot.core.model.ExportSnapshot;
import com.anchor.migration.javaastssot.profile.jpa.model.JpaEntityRecord;
import com.anchor.migration.javaastssot.profile.jpa.model.JpaFieldRecord;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class JpaAnnotationExtractor {

    private final JavaParser parser;

    JpaAnnotationExtractor() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_1_4);
        this.parser = new JavaParser(config);
    }

    void parseFile(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
        String source = Files.readString(file);
        CompilationUnit cu =
                parser.parse(source).getResult().orElseThrow(() -> new IllegalStateException("Failed to parse: " + file));
        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

        JpaSnapshot data = snapshot.requireProfile(JpaProfile.ID, JpaSnapshot.class);
        for (TypeDeclaration<?> type : cu.getTypes()) {
            extractType(type, relative, pkg, data);
        }
    }

    boolean containsEntityAnnotation(Path file) {
        try {
            String source = Files.readString(file);
            return source.contains("@Entity") || source.contains("persistence.Entity");
        } catch (Exception ex) {
            return false;
        }
    }

    private void extractType(String relativePath, String pkg, ClassOrInterfaceDeclaration type, JpaSnapshot data) {
        if (type.isInterface() || !hasAnnotation(type, "Entity")) {
            return;
        }

        String typeStableId = StableIds.javaType(pkg, type.getNameAsString());
        String tableName =
                findAnnotation(type, "Table")
                        .flatMap(ann -> stringMember(ann, "name"))
                        .orElse(type.getNameAsString());

        data.entities.add(new JpaEntityRecord(relativePath, typeStableId, tableName));

        for (FieldDeclaration field : type.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                extractField(typeStableId, field, var, data);
            }
        }
    }

    private void extractType(TypeDeclaration<?> type, String relativePath, String pkg, JpaSnapshot data) {
        if (type instanceof ClassOrInterfaceDeclaration classOrInterface) {
            extractType(relativePath, pkg, classOrInterface, data);
            for (var member : classOrInterface.getMembers()) {
                if (member instanceof TypeDeclaration<?> nested) {
                    extractType(nested, relativePath, pkg, data);
                }
            }
        }
    }

    private void extractField(
            String typeStableId, FieldDeclaration field, VariableDeclarator var, JpaSnapshot data) {
        boolean idField = hasAnnotation(field, "Id");
        Optional<String> columnName = findAnnotation(field, "Column").flatMap(ann -> stringMember(ann, "name"));
        if (columnName.isEmpty() && idField) {
            columnName = Optional.of(var.getNameAsString());
        }
        if (columnName.isEmpty() && hasAnnotation(field, "Column")) {
            columnName = Optional.of(var.getNameAsString());
        }
        if (columnName.isEmpty()) {
            return;
        }

        data.fields.add(new JpaFieldRecord(typeStableId, var.getNameAsString(), columnName.get(), idField));
    }

    private static boolean hasAnnotation(FieldDeclaration field, String simpleName) {
        return field.getAnnotations().stream().anyMatch(ann -> isAnnotationNamed(ann, simpleName));
    }

    private static boolean hasAnnotation(ClassOrInterfaceDeclaration type, String simpleName) {
        return type.getAnnotations().stream().anyMatch(ann -> isAnnotationNamed(ann, simpleName));
    }

    private static Optional<AnnotationExpr> findAnnotation(ClassOrInterfaceDeclaration type, String simpleName) {
        return type.getAnnotations().stream().filter(ann -> isAnnotationNamed(ann, simpleName)).findFirst();
    }

    private static Optional<AnnotationExpr> findAnnotation(FieldDeclaration field, String simpleName) {
        return field.getAnnotations().stream().filter(ann -> isAnnotationNamed(ann, simpleName)).findFirst();
    }

    private static boolean isAnnotationNamed(AnnotationExpr ann, String simpleName) {
        String name = ann.getNameAsString();
        return name.equals(simpleName) || name.endsWith("." + simpleName);
    }

    private static Optional<String> stringMember(AnnotationExpr ann, String memberName) {
        if (ann.isNormalAnnotationExpr()) {
            NormalAnnotationExpr normal = ann.asNormalAnnotationExpr();
            for (MemberValuePair pair : normal.getPairs()) {
                if (pair.getNameAsString().equals(memberName)) {
                    return literalAsString(pair.getValue());
                }
            }
            return Optional.empty();
        }
        if (ann.isSingleMemberAnnotationExpr() && "value".equals(memberName)) {
            return literalAsString(ann.asSingleMemberAnnotationExpr().getMemberValue());
        }
        return Optional.empty();
    }

    private static Optional<String> literalAsString(Expression expression) {
        if (expression.isStringLiteralExpr()) {
            return Optional.of(expression.asStringLiteralExpr().asString());
        }
        if (expression.isFieldAccessExpr()) {
            return Optional.of(expression.asFieldAccessExpr().getNameAsString());
        }
        if (expression.isNameExpr()) {
            return Optional.of(expression.asNameExpr().getNameAsString());
        }
        return Optional.empty();
    }
}
