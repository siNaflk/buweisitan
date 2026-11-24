-- M2增量脚本：仅新增点赞关系表，禁止重跑会DROP旧表的xunye.sql。
USE xunye;
CREATE TABLE IF NOT EXISTS comment_like (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    comment_id BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_comment(user_id,comment_id),
    KEY idx_comment_id(comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='M2评论点赞关系（取消点赞物理删除）';
