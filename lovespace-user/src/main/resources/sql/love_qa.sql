-- 恋爱知识库多轮问答：会话与消息持久化（供历史查看）
CREATE TABLE IF NOT EXISTS love_qa_conversations (
  conversation_id VARCHAR(64) NOT NULL COMMENT '与 API 返回的 conversationId 一致（UUID）',
  user_id VARCHAR(64) NOT NULL COMMENT '发起用户 ID（users.id）',
  couple_id VARCHAR(64) NULL COMMENT '可选情侣绑定 ID（couple_binding.id）',
  title VARCHAR(200) NULL COMMENT '首条用户消息摘要',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (conversation_id),
  KEY idx_love_qa_conv_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS love_qa_messages (
  id BIGINT NOT NULL AUTO_INCREMENT,
  conversation_id VARCHAR(64) NOT NULL,
  role VARCHAR(16) NOT NULL COMMENT 'user | assistant',
  content MEDIUMTEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_love_qa_msg_conv_created (conversation_id, created_at),
  CONSTRAINT fk_love_qa_msg_conv FOREIGN KEY (conversation_id) REFERENCES love_qa_conversations(conversation_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
