package com.meng.lovespace.ai.rag.retrieve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class RagSimilarityFilterTest {

    @Test
    void scoreFromDocument_convertsCosineDistance() {
        Document doc = docWithDistance(0.0);
        assertEquals(1.0, RagSimilarityFilter.scoreFromDocument(doc));
        assertEquals(0.5, RagSimilarityFilter.scoreFromDocument(docWithDistance(1.0)));
    }

    @Test
    void passesThreshold_filtersLowScoreChunks() {
        assertTrue(RagSimilarityFilter.passesThreshold(docWithDistance(0.5), 0.55));
        assertFalse(RagSimilarityFilter.passesThreshold(docWithDistance(1.2), 0.55));
    }

    @Test
    void filterAndLimit_respectsThresholdAndTopK() {
        List<Document> candidates =
                List.of(
                        docWithDistance(0.0),
                        docWithDistance(0.2),
                        docWithDistance(1.5),
                        docWithDistance(0.4));

        List<Document> result = RagSimilarityFilter.filterAndLimit(candidates, 0.55, 2);
        assertEquals(2, result.size());
        assertFalse(result.contains(docWithDistance(1.5)));
    }

    private static Document docWithDistance(double distance) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("distance", distance);
        return new Document("id-" + distance, "text", meta);
    }
}
