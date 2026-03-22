-- 恋爱时间轴记录
-- visibility: 1=仅自己可见, 2=情侣双方可见
CREATE TABLE IF NOT EXISTS love_records (
  id VARCHAR(64) NOT NULL,
  couple_id VARCHAR(64) NOT NULL COMMENT '情侣绑定 ID（couple_binding.id）',
  author_id VARCHAR(64) NOT NULL COMMENT '作者用户 ID',
  record_date DATE NOT NULL COMMENT '记录日期',
  content TEXT NOT NULL COMMENT '文字内容',
  mood VARCHAR(20) NOT NULL COMMENT '心情: happy,sad,excited,calm,loved,missed',
  location_json JSON NULL COMMENT '位置信息 JSON',
  visibility TINYINT NOT NULL DEFAULT 2 COMMENT '1仅自己 2情侣',
  tags_json JSON NULL COMMENT '标签 JSON 数组等',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_love_records_couple_date (couple_id, record_date),
  KEY idx_love_records_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
