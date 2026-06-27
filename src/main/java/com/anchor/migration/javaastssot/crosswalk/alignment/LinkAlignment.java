package com.anchor.migration.javaastssot.crosswalk.alignment;

public record LinkAlignment(
        String nameDriftClass,
        String typeRelationForward,
        String typeRelationBackward,
        String colorForward,
        String colorBackward,
        String roundTripClass,
        String normalizedSource,
        String normalizedTarget) {}
