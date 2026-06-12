package com.meng.lovespace.ai.rag.retrieve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RagRetrievalFilterBuilderTest {

    @Test
    void coupleWithGlobalFallback_includesCoupleAndGlobal() {
        String filter = RagRetrievalFilterBuilder.coupleWithGlobalFallback("couple-a");
        assertEquals("(coupleId == 'couple-a' || scope == 'GLOBAL')", filter);
    }

    @Test
    void coupleOnly_excludesOtherCouples() {
        String filterA = RagRetrievalFilterBuilder.coupleOnly("couple-a");
        String filterB = RagRetrievalFilterBuilder.coupleOnly("couple-b");
        assertFalse(filterA.contains("couple-b"));
        assertFalse(filterB.contains("couple-a"));
        assertEquals("coupleId == 'couple-a'", filterA);
    }

    @Test
    void coupleFilters_doNotCrossMatch() {
        String filterA = RagRetrievalFilterBuilder.coupleWithGlobalFallback("couple-a");
        assertTrue(filterA.contains("couple-a"));
        assertFalse(filterA.contains("couple-b"));
    }

    @Test
    void globalOnly_restrictsToPublicScope() {
        assertEquals("scope == 'GLOBAL'", RagRetrievalFilterBuilder.globalOnly());
    }

    @Test
    void escape_handlesSingleQuoteInCoupleId() {
        assertEquals("coupleId == 'abc\\'def'", RagRetrievalFilterBuilder.coupleOnly("abc'def"));
    }

    @Test
    void coupleOnly_rejectsBlankCoupleId() {
        assertThrows(IllegalArgumentException.class, () -> RagRetrievalFilterBuilder.coupleOnly("  "));
    }
}
