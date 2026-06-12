-- 恋爱知识库文档台账（RAG 摄入阶段 1：可追溯、可管理）
CREATE TABLE IF NOT EXISTS love_qa_documents (
  document_id VARCHAR(36) NOT NULL COMMENT 'UUID，与 Milvus metadata.documentId 一致',
  couple_id VARCHAR(64) NULL COMMENT '情侣私有；NULL 表示 GLOBAL',
  owner_user_id VARCHAR(64) NOT NULL COMMENT '入库用户 ID',
  title VARCHAR(256) NULL COMMENT '文档标题',
  source_url VARCHAR(1024) NULL COMMENT '来源 URL',
  category VARCHAR(64) NULL COMMENT '分类',
  scope VARCHAR(16) NOT NULL DEFAULT 'COUPLE' COMMENT 'COUPLE | GLOBAL',
  content MEDIUMTEXT NULL COMMENT '全文快照，供 reingest 使用',
  content_hash VARCHAR(64) NOT NULL COMMENT '全文 SHA-256（十六进制），文档级去重用',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | PROCESSING | SUCCESS | FAILED',
  chunk_count INT NOT NULL DEFAULT 0 COMMENT '成功写入 Milvus 的 chunk 数',
  error_message TEXT NULL COMMENT '失败原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (document_id),
  KEY idx_love_qa_doc_couple_updated (couple_id, updated_at),
  KEY idx_love_qa_doc_owner_updated (owner_user_id, updated_at),
  KEY idx_love_qa_doc_content_hash (content_hash),
  KEY idx_love_qa_doc_source_couple (source_url(255), couple_id),
  FULLTEXT INDEX ft_love_qa_doc_search (title, category, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
