-- 计划消费记录（与 couple_plans 关联；删除计划时级联删除）
CREATE TABLE IF NOT EXISTS plan_expenses (
  id VARCHAR(64) NOT NULL,
  plan_id VARCHAR(64) NOT NULL COMMENT '计划 ID（couple_plans.id）',
  expense_type VARCHAR(32) NOT NULL COMMENT 'lodging|transport|dining|other',
  amount DECIMAL(14, 2) NOT NULL,
  spent_date DATE NULL COMMENT '消费发生日',
  note VARCHAR(500) NULL,
  created_by VARCHAR(64) NOT NULL COMMENT '记录人用户 ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_plan_expenses_plan (plan_id),
  KEY idx_plan_expenses_created (created_at),
  CONSTRAINT fk_plan_expenses_plan FOREIGN KEY (plan_id) REFERENCES couple_plans (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
