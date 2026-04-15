package com.meng.lovespace.ai.travel;

import com.meng.lovespace.ai.config.LovespaceAiProperties;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 旅游场景下基于 Milvus {@code travel_poi_embeddings} 的语义检索骨架（占位：未入库前始终返回空）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPoiVectorSearchService {

    private final LovespaceAiProperties lovespaceAiProperties;

    /**
     * @param query 自然语言查询，如「浪漫的海边餐厅」
     * @param topK 条数
     * @return 检索到的 POI 文本摘要
     */
    public List<String> searchPoiHints(String query, int topK) {
        if (!lovespaceAiProperties.getTravel().isPoiVectorSearchEnabled()) {
            return Collections.emptyList();
        }
        log.debug("searchPoiHints skeleton: query={}, topK={}", query, topK);
        // TODO: embed query + Milvus search on travel_poi_embeddings
        return Collections.emptyList();
    }
}
