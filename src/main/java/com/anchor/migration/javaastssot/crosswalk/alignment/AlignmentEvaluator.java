package com.anchor.migration.javaastssot.crosswalk.alignment;

public final class AlignmentEvaluator {

    private AlignmentEvaluator() {}

    public static LinkAlignment evaluateNames(String sourceName, String targetName, boolean typeLabel) {
        String normalizedSource =
                typeLabel ? NameNormalizer.normalizeTypeLabel(sourceName) : NameNormalizer.normalizeIdentifier(sourceName);
        String normalizedTarget =
                typeLabel ? NameNormalizer.normalizeTypeLabel(targetName) : NameNormalizer.normalizeIdentifier(targetName);
        String nameDrift = NameDriftClassifier.classify(sourceName, targetName, typeLabel);
        String nameColor = colorForNameDrift(nameDrift);
        return new LinkAlignment(
                nameDrift,
                TypeRelations.UNKNOWN,
                TypeRelations.UNKNOWN,
                nameColor,
                nameColor,
                roundTripFromColors(nameColor, nameColor),
                normalizedSource,
                normalizedTarget);
    }

    public static LinkAlignment evaluateFieldMapping(
            String fieldName, String columnName, String javaFieldType, String sqlDataType) {
        String normalizedSource = NameNormalizer.normalizeIdentifier(fieldName);
        String normalizedTarget = NameNormalizer.normalizeIdentifier(columnName);
        String nameDrift = NameDriftClassifier.classify(fieldName, columnName, false);

        LogicalType physical = TypeParser.fromSqlDataType(sqlDataType);
        LogicalType persistence = TypeParser.fromJavaType(javaFieldType);
        String typeForward = TypeRelationEvaluator.forward(physical, persistence);
        String typeBackward = TypeRelationEvaluator.backward(persistence, physical);

        String colorForward = combine(nameDrift, colorForTypeRelation(typeForward));
        String colorBackward = combine(nameDrift, colorForTypeRelation(typeBackward));

        return new LinkAlignment(
                nameDrift,
                typeForward,
                typeBackward,
                colorForward,
                colorBackward,
                roundTripFromColors(colorForward, colorBackward),
                normalizedSource,
                normalizedTarget);
    }

    private static String colorForNameDrift(String nameDrift) {
        return switch (nameDrift) {
            case NameDriftClasses.NONE -> EdgeColors.GREEN;
            case NameDriftClasses.EXPLAINABLE -> EdgeColors.YELLOW;
            case NameDriftClasses.QUESTIONABLE -> EdgeColors.ORANGE;
            case NameDriftClasses.UNEXPLAINABLE -> EdgeColors.RED;
            default -> EdgeColors.RED;
        };
    }

    private static String colorForTypeRelation(String relation) {
        return switch (relation) {
            case TypeRelations.EXACT, TypeRelations.UNKNOWN -> EdgeColors.GREEN;
            case TypeRelations.WIDENING, TypeRelations.STRINGIFIED -> EdgeColors.GREEN;
            case TypeRelations.NARROWING, TypeRelations.PARSED -> EdgeColors.YELLOW;
            case TypeRelations.INCOMPATIBLE -> EdgeColors.RED;
            default -> EdgeColors.ORANGE;
        };
    }

    private static String combine(String nameDrift, String typeColor) {
        if (NameDriftClasses.UNEXPLAINABLE.equals(nameDrift)) {
            return EdgeColors.RED;
        }
        return EdgeColors.max(colorForNameDrift(nameDrift), typeColor);
    }

    private static String roundTripFromColors(String colorForward, String colorBackward) {
        if (EdgeColors.severity(colorForward) >= EdgeColors.severity(EdgeColors.RED)
                || EdgeColors.severity(colorBackward) >= EdgeColors.severity(EdgeColors.RED)) {
            return RoundTripClasses.INCOMPATIBLE;
        }
        if (EdgeColors.severity(colorBackward) >= EdgeColors.severity(EdgeColors.ORANGE)) {
            return RoundTripClasses.UNSAFE_BACKWARD;
        }
        if (EdgeColors.GREEN.equals(colorForward) && EdgeColors.GREEN.equals(colorBackward)) {
            return RoundTripClasses.SAFE;
        }
        if (EdgeColors.GREEN.equals(colorForward) && EdgeColors.YELLOW.equals(colorBackward)) {
            return RoundTripClasses.ASYMMETRIC;
        }
        return RoundTripClasses.ASYMMETRIC;
    }
}
