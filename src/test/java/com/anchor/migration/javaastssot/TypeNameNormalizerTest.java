package com.anchor.migration.javaastssot;

import com.anchor.migration.javaastssot.listusage.TypeNameNormalizer;
import com.anchor.migration.javaastssot.listusage.UsageClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeNameNormalizerTest {

    @Test
    void primitiveAndBoxedAreCompatible() {
        assertTrue(TypeNameNormalizer.areCompatible("int", "Integer"));
    }

    @Test
    void distinctTypesAreTuple() {
        assertEquals(
                UsageClass.tuple,
                TypeNameNormalizer.classifyTypes(List.of("String", "Integer")));
    }

    @Test
    void emptyEvidenceIsUnknown() {
        assertEquals(UsageClass.unknown, TypeNameNormalizer.classifyTypes(List.of()));
    }
}
