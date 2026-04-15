package com.meng.lovespace.ai.travel;

import java.util.Collections;
import java.util.List;

/**
 * 高德地图 Web 服务占位：后续可在此封装关键词搜索 / 周边 POI 等，仅作行程 enrich。
 */
public interface AmapPlacesClient {

    /**
     * @param city 城市名
     * @param keywords 关键词
     * @return 简要 POI 描述列表（占位实现返回空）
     */
    List<String> searchHints(String city, String keywords);

    /** 默认空实现 Bean。 */
    final class NoOp implements AmapPlacesClient {
        @Override
        public List<String> searchHints(String city, String keywords) {
            return Collections.emptyList();
        }
    }
}
