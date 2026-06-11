package com.meng.lovespace.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 恋爱知识库文档台账，表 {@code love_qa_documents}。 */
@Data
@TableName("love_qa_documents")
public class LoveQaDocument {

    @TableId(value = "document_id", type = IdType.INPUT)
    private String documentId;

    @TableField("couple_id")
    private String coupleId;

    @TableField("owner_user_id")
    private String ownerUserId;

    @TableField("title")
    private String title;

    @TableField("source_url")
    private String sourceUrl;

    @TableField("category")
    private String category;

    @TableField("scope")
    private String scope;

    @TableField("content")
    private String content;

    @TableField("content_hash")
    private String contentHash;

    @TableField("status")
    private String status;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("error_message")
    private String errorMessage;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
