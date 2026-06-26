package com.anchor.migration.javaastssot.core.extract;

import com.anchor.migration.javaastssot.core.StableIds;
import com.anchor.migration.javaastssot.core.model.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class JavaSourceExtractor {

    private final JavaParser parser;

    public JavaSourceExtractor() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_1_4);
        this.parser = new JavaParser(config);
    }

    public void parseFile(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
        snapshot.sourceFiles.add(new SourceFileRecord(relative, "java"));
        snapshot.javaFileCount++;

        String source = Files.readString(file);
        CompilationUnit cu = parser.parse(source).getResult().orElseThrow(
                () -> new IllegalStateException("Failed to parse: " + file));

        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

        for (ImportDeclaration imp : cu.getImports()) {
            snapshot.javaImports.add(
                    new JavaImportRecord(
                            relative,
                            imp.getNameAsString(),
                            imp.isStatic(),
                            imp.isAsterisk()));
        }

        for (TypeDeclaration<?> type : cu.getTypes()) {
            extractType(type, relative, pkg, null, snapshot);
        }
    }

    private void extractType(
            TypeDeclaration<?> type,
            String relativePath,
            String pkg,
            String enclosingStableId,
            ExportSnapshot snapshot) {
        if (type instanceof ClassOrInterfaceDeclaration classOrInterface) {
            extractClassOrInterface(classOrInterface, relativePath, pkg, enclosingStableId, snapshot);
        } else if (type instanceof EnumDeclaration enumDecl) {
            addType(enumDecl, relativePath, pkg, enclosingStableId, "enum", null, null, snapshot);
        }
    }

    private void extractClassOrInterface(
            ClassOrInterfaceDeclaration type,
            String relativePath,
            String pkg,
            String enclosingStableId,
            ExportSnapshot snapshot) {
        String kind = type.isInterface() ? "interface" : "class";
        String extendsType =
                type.getExtendedTypes().stream()
                        .findFirst()
                        .map(ClassOrInterfaceType::asString)
                        .orElse(null);
        String implementsList =
                type.getImplementedTypes().stream()
                        .map(ClassOrInterfaceType::asString)
                        .collect(Collectors.joining(","));
        String stableId =
                addType(type, relativePath, pkg, enclosingStableId, kind, extendsType, implementsList, snapshot);

        for (MethodDeclaration method : type.getMethods()) {
            String modifiers = method.getModifiers().stream()
                    .map(Modifier::getKeyword)
                    .map(Object::toString)
                    .collect(Collectors.joining(" "));
            String parameterTypes = method.getParameters().stream()
                    .map(p -> p.getType().asString())
                    .collect(Collectors.joining(","));
            snapshot.javaMethods.add(
                    new JavaMethodRecord(
                            stableId,
                            StableIds.javaMethod(stableId, method.getNameAsString(), parameterTypes),
                            method.getNameAsString(),
                            method.getType().asString(),
                            modifiers));
        }

        for (FieldDeclaration field : type.getFields()) {
            String modifiers = field.getModifiers().stream()
                    .map(Modifier::getKeyword)
                    .map(Object::toString)
                    .collect(Collectors.joining(" "));
            for (VariableDeclarator var : field.getVariables()) {
                snapshot.javaFields.add(
                        new JavaFieldRecord(
                                stableId,
                                StableIds.javaField(stableId, var.getNameAsString()),
                                var.getNameAsString(),
                                var.getType().asString(),
                                modifiers));
            }
        }

        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                extractType(nested, relativePath, pkg, stableId, snapshot);
            }
        }
    }

    private String addType(
            BodyDeclaration<?> type,
            String relativePath,
            String pkg,
            String enclosingStableId,
            String kind,
            String extendsType,
            String implementsList,
            ExportSnapshot snapshot) {
        String simpleName =
                type instanceof ClassOrInterfaceDeclaration c
                        ? c.getNameAsString()
                        : type instanceof EnumDeclaration e ? e.getNameAsString() : "Unknown";
        String stableId =
                enclosingStableId == null
                        ? StableIds.javaType(pkg, simpleName)
                        : enclosingStableId + "$" + simpleName;
        snapshot.javaTypes.add(
                new JavaTypeRecord(
                        relativePath, stableId, pkg, simpleName, kind, extendsType, implementsList));
        return stableId;
    }
}
