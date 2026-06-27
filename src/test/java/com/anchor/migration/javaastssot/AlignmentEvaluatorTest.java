package com.anchor.migration.javaastssot;

import com.anchor.migration.javaastssot.crosswalk.alignment.AlignmentEvaluator;
import com.anchor.migration.javaastssot.crosswalk.alignment.EdgeColors;
import com.anchor.migration.javaastssot.crosswalk.alignment.LinkAlignment;
import com.anchor.migration.javaastssot.crosswalk.alignment.NameDriftClasses;
import com.anchor.migration.javaastssot.crosswalk.alignment.RoundTripClasses;
import com.anchor.migration.javaastssot.crosswalk.alignment.TypeRelations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlignmentEvaluatorTest {

    @Test
    void conventionOnlyFieldNamesAreGreenBothWays() {
        LinkAlignment alignment = AlignmentEvaluator.evaluateFieldMapping("accountId", "ACCOUNT_ID", null, "varchar");
        assertEquals(NameDriftClasses.NONE, alignment.nameDriftClass());
        assertEquals(EdgeColors.GREEN, alignment.colorForward());
        assertEquals(EdgeColors.GREEN, alignment.colorBackward());
        assertEquals(RoundTripClasses.SAFE, alignment.roundTripClass());
    }

    @Test
    void intToLongIsGreenForwardYellowBackward() {
        LinkAlignment alignment = AlignmentEvaluator.evaluateFieldMapping("id", "ID", "long", "int");
        assertEquals(TypeRelations.WIDENING, alignment.typeRelationForward());
        assertEquals(TypeRelations.NARROWING, alignment.typeRelationBackward());
        assertEquals(EdgeColors.GREEN, alignment.colorForward());
        assertEquals(EdgeColors.YELLOW, alignment.colorBackward());
        assertEquals(RoundTripClasses.ASYMMETRIC, alignment.roundTripClass());
    }

    @Test
    void exactTypesAreGreenBothWays() {
        LinkAlignment alignment = AlignmentEvaluator.evaluateFieldMapping("name", "NAME", "String", "varchar");
        assertEquals(TypeRelations.EXACT, alignment.typeRelationForward());
        assertEquals(TypeRelations.EXACT, alignment.typeRelationBackward());
        assertEquals(EdgeColors.GREEN, alignment.colorForward());
        assertEquals(EdgeColors.GREEN, alignment.colorBackward());
    }

    @Test
    void unexplainableNamesAreRedBothWays() {
        LinkAlignment alignment = AlignmentEvaluator.evaluateFieldMapping("userId", "orderId", "String", "varchar");
        assertEquals(NameDriftClasses.UNEXPLAINABLE, alignment.nameDriftClass());
        assertEquals(EdgeColors.RED, alignment.colorForward());
        assertEquals(EdgeColors.RED, alignment.colorBackward());
        assertEquals(RoundTripClasses.INCOMPATIBLE, alignment.roundTripClass());
    }

    @Test
    void typeTableNamesStripBeanSuffix() {
        LinkAlignment alignment = AlignmentEvaluator.evaluateNames("AccountBean", "ACCOUNT", true);
        assertEquals(NameDriftClasses.NONE, alignment.nameDriftClass());
        assertEquals("account", alignment.normalizedSource());
        assertEquals("account", alignment.normalizedTarget());
    }

    @Test
    void explainableAliasIsYellowBothWaysWhenTypesUnknown() {
        LinkAlignment alignment = AlignmentEvaluator.evaluateNames("acct", "account", false);
        assertEquals(NameDriftClasses.EXPLAINABLE, alignment.nameDriftClass());
        assertEquals(EdgeColors.YELLOW, alignment.colorForward());
        assertEquals(EdgeColors.YELLOW, alignment.colorBackward());
    }
}
