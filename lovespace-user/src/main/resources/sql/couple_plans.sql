-- 共同计划与任务
CREATE TABLE IF NOT EXISTS couple_plans (
  id VARCHAR(64) NOT NULL,
  couple_id VARCHAR(64) NOT NULL COMMENT '情侣绑定 ID（couple_binding.id）',
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  plan_type VARCHAR(32) NOT NULL COMMENT 'goal|travel|event',
  priority INT NOT NULL DEFAULT 0,
  start_date DATE NULL,
  end_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  progress INT NOT NULL DEFAULT 0 COMMENT '0-100',
  budget_total DECIMAL(14, 2) NULL,
  budget_spent DECIMAL(14, 2) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_couple_plans_couple (couple_id),
  KEY idx_couple_plans_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS plan_tasks (
  id VARCHAR(64) NOT NULL,
  plan_id VARCHAR(64) NOT NULL COMMENT '计划 ID（couple_plans.id）',
  title VARCHAR(200) NOT NULL,
  assignee_id VARCHAR(64) NULL COMMENT '负责人用户 ID（须为情侣成员之一）',
  is_completed TINYINT NOT NULL DEFAULT 0 COMMENT '0 未完成 1 已完成',
  completed_at DATETIME NULL,
  due_date DATE NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_plan_tasks_plan (plan_id),
  KEY idx_plan_tasks_assignee (assignee_id),
  CONSTRAINT fk_plan_tasks_plan FOREIGN KEY (plan_id) REFERENCES couple_plans (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
