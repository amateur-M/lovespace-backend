package com.meng.lovespace.user.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** MyBatis FULLTEXT 检索行映射。 */
@Data
public class LoveQaKeywordSearchRow {

    private String documentId;
    private String title;
    private String category;
    private String scope;
    private String coupleId;
    private String content;
    private LocalDateTime updatedAt;
    private Double bm25Score;
}
