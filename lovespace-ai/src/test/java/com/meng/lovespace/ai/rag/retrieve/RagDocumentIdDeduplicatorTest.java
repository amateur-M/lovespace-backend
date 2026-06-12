package com.meng.lovespace.ai.rag.retrieve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class RagDocumentIdDeduplicatorTest {

    @Test
    void dedupeKeepBestScore_keepsHighestSimilarityPerDocument() {
        Document low =
                doc("c1", "doc-a", 1.0);
        Document high =
                doc("c2", "doc-a", 0.2);
        Document other =
                doc("c3", "doc-b", 0.3);

        List<Document> result = RagDocumentIdDeduplicator.dedupeKeepBestScore(List.of(low, high, other));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(d -> "c2".equals(d.getId())));
        assertTrue(result.stream().anyMatch(d -> "c3".equals(d.getId())));
    }

    @Test
    void dedupeKeepBestScore_preservesFirstSeenDocumentSlot() {
        Document a1 = doc("a1", "doc-a", 0.1);
        Document b1 = doc("b1", "doc-b", 0.2);
        Document a2 = doc("a2", "doc-a", 0.9);

        List<Document> result = RagDocumentIdDeduplicator.dedupeKeepBestScore(List.of(a1, b1, a2));

        assertEquals(2, result.size());
        assertEquals("a2", result.get(0).getId());
        assertEquals("b1", result.get(1).getId());
    }

    private static Document doc(String id, String documentId, double distance) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("documentId", documentId);
        meta.put("distance", distance);
        return new Document(id, "text-" + id, meta);
    }
}
