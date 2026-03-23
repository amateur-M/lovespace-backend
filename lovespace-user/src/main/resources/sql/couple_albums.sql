-- 情侣相册与照片
CREATE TABLE IF NOT EXISTS couple_albums (
  id VARCHAR(64) NOT NULL,
  couple_id VARCHAR(64) NOT NULL COMMENT '情侣绑定 ID（couple_binding.id）',
  name VARCHAR(128) NOT NULL COMMENT '相册名称',
  cover_image_url VARCHAR(500) NULL COMMENT '封面图 URL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_couple_albums_couple (couple_id),
  KEY idx_couple_albums_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS album_photos (
  id VARCHAR(64) NOT NULL,
  album_id VARCHAR(64) NOT NULL COMMENT '相册 ID（couple_albums.id）',
  uploader_id VARCHAR(64) NOT NULL COMMENT '上传者用户 ID',
  image_url VARCHAR(500) NOT NULL COMMENT '原图 URL',
  thumbnail_url VARCHAR(500) NULL COMMENT '缩略图 URL',
  description VARCHAR(500) NULL COMMENT '照片描述',
  location_json JSON NULL COMMENT '位置信息 JSON',
  taken_date DATE NULL COMMENT '拍摄日期',
  tags_json JSON NULL COMMENT '标签 JSON',
  is_favorite TINYINT NOT NULL DEFAULT 0 COMMENT '是否收藏：0否 1是',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_album_photos_album_created (album_id, created_at),
  KEY idx_album_photos_uploader (uploader_id),
  KEY idx_album_photos_favorite (album_id, is_favorite)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
