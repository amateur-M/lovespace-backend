package com.meng.lovespace.ai.rag.retrieve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.meng.lovespace.ai.rag.config.RagAiProperties;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class RagL1RerankerTest {

    private RagAiProperties props;
    private RagL1Reranker reranker;

    @BeforeEach
    void setUp() {
        props = new RagAiProperties();
        props.setRetrieveTopK(2);
        props.setRerankEnabled(true);
        props.setRerankCategoryBoost(0.1);
        props.setRerankRecencyBoost(0.1);
        props.setRerankRecencyDays(30);
        reranker = new RagL1Reranker(props);
    }

    @Test
    void rerank_boostsMatchingCategory() {
        Document generic = doc("g", null, null, 0.0);
        Document conflict = doc("c", "冲突解决", null, 0.4);

        List<Document> result = reranker.rerank("冲突解决有什么好办法", List.of(generic, conflict));

        assertEquals(2, result.size());
        assertEquals("c", result.get(0).getId());
    }

    @Test
    void rerank_boostsRecentIngest() {
        Document oldDoc = doc("old", null, Instant.now().minusSeconds(86400L * 60).toString(), 0.4);
        Document newDoc = doc("new", null, Instant.now().minusSeconds(3600).toString(), 0.44);

        List<Document> result = reranker.rerank("通用问题", List.of(oldDoc, newDoc));

        assertEquals("new", result.get(0).getId());
    }

    @Test
    void rerank_respectsTopK() {
        List<Document> docs =
                List.of(
                        doc("1", null, null, 0.0),
                        doc("2", null, null, 0.1),
                        doc("3", null, null, 0.2));

        List<Document> result = reranker.rerank("q", docs);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(d -> "1".equals(d.getId())));
    }

    private static Document doc(String id, String category, String ingestedAt, double distance) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("distance", distance);
        if (category != null) {
            meta.put("category", category);
        }
        if (ingestedAt != null) {
            meta.put("ingestedAt", ingestedAt);
        }
        return new Document(id, "text", meta);
    }
}
