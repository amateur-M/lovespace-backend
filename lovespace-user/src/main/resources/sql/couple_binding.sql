-- 情侣绑定表
-- status: 0=待接受邀请（邀请流程）, 1=交往中(active), 2=冻结(frozen), 3=已解除(separated)
CREATE TABLE IF NOT EXISTS couple_binding (
  id VARCHAR(64) NOT NULL,
  user_id1 VARCHAR(64) NOT NULL COMMENT '用户1（邀请发起方 / 成对后字典序较小的一方）',
  user_id2 VARCHAR(64) NOT NULL COMMENT '用户2（被邀请方 / 成对后字典序较大的一方）',
  start_date DATE NULL COMMENT '恋爱开始日期，接受邀请或后续更新',
  relationship_days INT NOT NULL DEFAULT 0 COMMENT '恋爱天数（由 start_date 与当前日期计算后回写）',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待接受 1交往 2冻结 3解除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_couple_binding_user1 (user_id1),
  KEY idx_couple_binding_user2 (user_id2),
  KEY idx_couple_binding_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
