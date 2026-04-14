-- 存量库迁移：手机号作为登录账号，邮箱改为可选（在个人资料中维护）
-- 执行前请备份。全新安装请直接使用 users.sql，无需执行本脚本。

ALTER TABLE users ADD COLUMN phone VARCHAR(20) NULL COMMENT '登录手机号' AFTER username;
ALTER TABLE users MODIFY COLUMN email VARCHAR(100) NULL;

-- 为尚无手机号的用户分配占位号（19900000001 起递增），上线后应引导用户改为真实号码
UPDATE users u
INNER JOIN (
  SELECT id, ROW_NUMBER() OVER (ORDER BY created_at) AS rn
  FROM users
  WHERE phone IS NULL
) t ON u.id = t.id
SET u.phone = CONCAT('199', LPAD(t.rn, 8, '0'));

ALTER TABLE users MODIFY COLUMN phone VARCHAR(20) NOT NULL;
ALTER TABLE users ADD UNIQUE KEY uk_users_phone (phone);
