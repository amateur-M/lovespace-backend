package com.meng.lovespace.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LoveQaKeywordRetrieverImplTest {

    @Test
    void prepareQuery_stripsFulltextSpecialChars() {
        assertEquals("冷战 怎么办", LoveQaKeywordRetrieverImpl.prepareQuery("冷战+怎么办"));
    }

    @Test
    void prepareQuery_rejectsTooShort() {
        assertNull(LoveQaKeywordRetrieverImpl.prepareQuery("a"));
    }

    @Test
    void prepareQuery_acceptsChinesePhrase() {
        assertEquals("道歉信怎么写", LoveQaKeywordRetrieverImpl.prepareQuery("  道歉信怎么写  "));
    }
}
