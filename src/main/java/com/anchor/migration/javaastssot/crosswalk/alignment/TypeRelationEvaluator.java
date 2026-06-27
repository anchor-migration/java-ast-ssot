package com.anchor.migration.javaastssot.crosswalk.alignment;

final class TypeRelationEvaluator {

    private TypeRelationEvaluator() {}

    /** Physical (SQL) → persistence (Java) — read / precision-up direction. */
    static String forward(LogicalType physical, LogicalType persistence) {
        return relation(physical, persistence);
    }

    /** Persistence (Java) → physical (SQL) — write / precision-down direction. */
    static String backward(LogicalType persistence, LogicalType physical) {
        return relation(persistence, physical);
    }

    private static String relation(LogicalType from, LogicalType to) {
        if (from == LogicalType.UNKNOWN || to == LogicalType.UNKNOWN) {
            return TypeRelations.UNKNOWN;
        }
        if (from == to) {
            return TypeRelations.EXACT;
        }
        if (from == LogicalType.STRING || to == LogicalType.STRING) {
            return from == LogicalType.STRING ? TypeRelations.PARSED : TypeRelations.STRINGIFIED;
        }
        if (isNumeric(from) && isNumeric(to)) {
            int fromRank = numericRank(from);
            int toRank = numericRank(to);
            if (toRank > fromRank) {
                return TypeRelations.WIDENING;
            }
            if (toRank < fromRank) {
                return TypeRelations.NARROWING;
            }
        }
        if (from == LogicalType.DATE && to == LogicalType.TIMESTAMP) {
            return TypeRelations.WIDENING;
        }
        if (from == LogicalType.TIMESTAMP && to == LogicalType.DATE) {
            return TypeRelations.NARROWING;
        }
        return TypeRelations.INCOMPATIBLE;
    }

    private static boolean isNumeric(LogicalType type) {
        return type == LogicalType.INT
                || type == LogicalType.LONG
                || type == LogicalType.DECIMAL
                || type == LogicalType.FLOAT
                || type == LogicalType.DOUBLE;
    }

    private static int numericRank(LogicalType type) {
        return switch (type) {
            case INT -> 1;
            case LONG -> 2;
            case FLOAT -> 3;
            case DOUBLE -> 4;
            case DECIMAL -> 5;
            default -> 0;
        };
    }
}
