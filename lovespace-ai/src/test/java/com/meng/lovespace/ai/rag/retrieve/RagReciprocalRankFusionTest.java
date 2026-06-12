package com.meng.lovespace.ai.rag.retrieve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class RagReciprocalRankFusionTest {

    @Test
    void fuse_boostsDocumentsPresentInBothLists() {
        Document vectorDoc = doc("v1", "doc-a", false);
        Document keywordDoc = doc("keyword-doc-a", "doc-a", true);

        List<Document> fused = RagReciprocalRankFusion.fuse(List.of(vectorDoc), List.of(keywordDoc), 60);

        assertEquals(1, fused.size());
        assertEquals("v1", fused.get(0).getId());
    }

    @Test
    void fuse_mergesDistinctDocuments() {
        Document v1 = doc("v1", "doc-a", false);
        Document k1 = doc("keyword-doc-b", "doc-b", true);

        List<Document> fused = RagReciprocalRankFusion.fuse(List.of(v1), List.of(k1), 60);

        assertEquals(2, fused.size());
    }

    @Test
    void fuse_prefersVectorChunkWhenSameDocumentId() {
        Document vectorDoc = doc("v1", "doc-a", false);
        Document keywordDoc = doc("keyword-doc-a", "doc-a", true);

        List<Document> fused =
                RagReciprocalRankFusion.fuse(List.of(vectorDoc), List.of(keywordDoc), 60);

        assertTrue(RagReciprocalRankFusion.isKeywordDoc(keywordDoc));
        assertEquals("v1", fused.get(0).getId());
    }

    private static Document doc(String id, String documentId, boolean keyword) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("documentId", documentId);
        if (keyword) {
            meta.put(LoveQaKeywordDocumentAdapter.RETRIEVAL_SOURCE, LoveQaKeywordDocumentAdapter.SOURCE_KEYWORD);
        } else {
            meta.put("distance", 0.2);
        }
        return new Document(id, "text", meta);
    }
}
