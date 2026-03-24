-- 私密消息
CREATE TABLE IF NOT EXISTS private_messages (
  id VARCHAR(64) NOT NULL,
  couple_id VARCHAR(64) NOT NULL COMMENT '情侣绑定 ID（couple_binding.id）',
  sender_id VARCHAR(64) NOT NULL COMMENT '发送方用户 ID',
  receiver_id VARCHAR(64) NOT NULL COMMENT '接收方用户 ID',
  content TEXT NOT NULL COMMENT '消息内容',
  message_type VARCHAR(20) NOT NULL COMMENT 'text/image/voice/letter',
  is_scheduled TINYINT NOT NULL DEFAULT 0 COMMENT '是否定时：0否 1是',
  scheduled_time DATETIME NULL COMMENT '定时发送时间',
  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0否 1是',
  read_time DATETIME NULL COMMENT '已读时间',
  is_retracted TINYINT NOT NULL DEFAULT 0 COMMENT '是否撤回：0否 1是',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_private_messages_couple_created (couple_id, created_at),
  KEY idx_private_messages_receiver_read (receiver_id, is_read),
  KEY idx_private_messages_scheduled (is_scheduled, scheduled_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
