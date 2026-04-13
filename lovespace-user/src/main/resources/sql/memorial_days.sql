-- 纪念日（按情侣维度共享；每年重复的月-日由 memorial_date 表示）
CREATE TABLE IF NOT EXISTS memorial_days (
  id VARCHAR(64) NOT NULL,
  couple_id VARCHAR(64) NOT NULL COMMENT '情侣绑定 ID（couple_binding.id）',
  user_id VARCHAR(64) NOT NULL COMMENT '创建人用户 ID',
  name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  memorial_date DATE NOT NULL COMMENT '纪念日日期（仅用月-日参与每年循环；年份可表示首次年份）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_memorial_days_couple (couple_id),
  KEY idx_memorial_days_user (user_id),
  KEY idx_memorial_days_date (memorial_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
