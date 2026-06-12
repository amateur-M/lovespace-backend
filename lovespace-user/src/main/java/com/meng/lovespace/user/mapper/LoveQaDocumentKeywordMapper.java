package com.meng.lovespace.user.mapper;

import com.meng.lovespace.user.dto.LoveQaKeywordSearchRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 恋爱知识库文档 MySQL FULLTEXT 检索（混合 RAG P2-C）。 */
@Mapper
public interface LoveQaDocumentKeywordMapper {

    @Select(
            """
            SELECT document_id, title, category, scope, couple_id, content, updated_at,
                   MATCH(title, category, content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) AS bm25_score
            FROM love_qa_documents
            WHERE status = 'SUCCESS'
              AND (couple_id = #{coupleId} OR scope = 'GLOBAL')
              AND MATCH(title, category, content) AGAINST(#{query} IN NATURAL LANGUAGE MODE)
            ORDER BY bm25_score DESC
            LIMIT #{limit}
            """)
    List<LoveQaKeywordSearchRow> searchCoupleWithGlobal(
            @Param("coupleId") String coupleId, @Param("query") String query, @Param("limit") int limit);

    @Select(
            """
            SELECT document_id, title, category, scope, couple_id, content, updated_at,
                   MATCH(title, category, content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) AS bm25_score
            FROM love_qa_documents
            WHERE status = 'SUCCESS'
              AND couple_id = #{coupleId}
              AND MATCH(title, category, content) AGAINST(#{query} IN NATURAL LANGUAGE MODE)
            ORDER BY bm25_score DESC
            LIMIT #{limit}
            """)
    List<LoveQaKeywordSearchRow> searchCoupleOnly(
            @Param("coupleId") String coupleId, @Param("query") String query, @Param("limit") int limit);

    @Select(
            """
            SELECT document_id, title, category, scope, couple_id, content, updated_at,
                   MATCH(title, category, content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) AS bm25_score
            FROM love_qa_documents
            WHERE status = 'SUCCESS'
              AND scope = 'GLOBAL'
              AND MATCH(title, category, content) AGAINST(#{query} IN NATURAL LANGUAGE MODE)
            ORDER BY bm25_score DESC
            LIMIT #{limit}
            """)
    List<LoveQaKeywordSearchRow> searchGlobalOnly(@Param("query") String query, @Param("limit") int limit);
}
