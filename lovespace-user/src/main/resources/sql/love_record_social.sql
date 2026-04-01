-- 恋爱记录点赞、评论（记录删除时级联清理）
CREATE TABLE IF NOT EXISTS love_record_likes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  record_id VARCHAR(64) NOT NULL COMMENT 'love_records.id',
  user_id VARCHAR(64) NOT NULL COMMENT '点赞用户',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_love_record_like (record_id, user_id),
  KEY idx_love_record_like_record (record_id),
  CONSTRAINT fk_love_record_like_record FOREIGN KEY (record_id) REFERENCES love_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS love_record_comments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  record_id VARCHAR(64) NOT NULL COMMENT 'love_records.id',
  user_id VARCHAR(64) NOT NULL COMMENT '评论作者',
  content VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_love_record_comment_record_time (record_id, created_at),
  CONSTRAINT fk_love_record_comment_record FOREIGN KEY (record_id) REFERENCES love_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
