package com.anchor.migration.javaastssot.listusage;

import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.Optional;

public enum CollectionKind {
    vector,
    array_list,
    linked_list,
    raw_list,
    stack,
    other;

    public static Optional<CollectionKind> fromType(Type type) {
        if (!(type instanceof ClassOrInterfaceType classType)) {
            return Optional.empty();
        }
        if (classType.getTypeArguments().isPresent() && !classType.getTypeArguments().get().isEmpty()) {
            return Optional.empty();
        }
        return fromSimpleName(classType.getNameAsString());
    }

    public static Optional<CollectionKind> fromSimpleName(String simpleName) {
        if (simpleName == null || simpleName.isBlank()) {
            return Optional.empty();
        }
        return switch (simpleName) {
            case "Vector" -> Optional.of(vector);
            case "ArrayList" -> Optional.of(array_list);
            case "LinkedList" -> Optional.of(linked_list);
            case "List" -> Optional.of(raw_list);
            case "Stack" -> Optional.of(stack);
            default -> Optional.empty();
        };
    }

    public String jsonValue() {
        return name();
    }
}
