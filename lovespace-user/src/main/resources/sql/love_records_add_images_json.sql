-- 已有库升级：为 love_records 增加图片字段（若列已存在可忽略报错后手动校验）
ALTER TABLE love_records
  ADD COLUMN images_json JSON NULL COMMENT '图片 URL 数组 JSON' AFTER tags_json;
