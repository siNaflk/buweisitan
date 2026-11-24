-- =============================================================
-- 寻页（XunYe）建库建表脚本
-- MySQL 8.0+，字符集 utf8mb4，引擎 InnoDB
-- 金额单位统一为"分"（BIGINT）
-- 约定：所有表含 id/create_time/update_time，业务表含 is_deleted
-- =============================================================

CREATE DATABASE IF NOT EXISTS `xunye` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `xunye`;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        VARCHAR(50)  NOT NULL COMMENT '用户名（登录凭证之一）',
    `phone`           VARCHAR(20)  NOT NULL COMMENT '手机号',
    `password`        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    `nickname`        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`          VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `balance`         BIGINT       NOT NULL DEFAULT 0 COMMENT '余额（分），仅钱包用途',
    `is_author`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 读者 / 1 作者',
    `vip_expire_time` DATETIME     DEFAULT NULL COMMENT 'VIP 到期时间，NULL 表示非会员',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 封禁',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_username` (`username`)
) ENGINE = InnoDB COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 阅读偏好表（与 user 一对一）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `reader_setting`;
CREATE TABLE `reader_setting` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `font_size`    TINYINT     NOT NULL DEFAULT 18 COMMENT '字号，默认 18',
    `font_family`  VARCHAR(20) NOT NULL DEFAULT 'default' COMMENT '字体',
    `text_color`   VARCHAR(10) NOT NULL DEFAULT '#333333' COMMENT '文字颜色',
    `bg_theme`     VARCHAR(20) NOT NULL DEFAULT 'white' COMMENT '阅读背景：white/beige/parchment/night',
    `ui_theme`     VARCHAR(20) NOT NULL DEFAULT 'light' COMMENT 'UI 主题：light/dark/vip',
    `line_spacing` TINYINT     NOT NULL DEFAULT 15 COMMENT '行间距倍数 ×10（15 = 1.5 倍）',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '阅读偏好表';

-- ------------------------------------------------------------
-- 3. 作品分类表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `book_category`;
CREATE TABLE `book_category` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(20) NOT NULL COMMENT '分类名',
    `sort`        TINYINT     NOT NULL DEFAULT 0 COMMENT '排序权重',
    `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '0 启用 / 1 停用',
    `is_deleted`  TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = '作品分类表';

-- ------------------------------------------------------------
-- 4. 作品表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `book`;
CREATE TABLE `book` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`          VARCHAR(100) NOT NULL COMMENT '书名',
    `author_id`      BIGINT UNSIGNED NOT NULL COMMENT '作者用户 ID',
    `author_name`    VARCHAR(50)  DEFAULT NULL COMMENT '作者名快照',
    `category_id`    BIGINT UNSIGNED NOT NULL COMMENT '分类 ID',
    `cover_url`      VARCHAR(255) DEFAULT NULL COMMENT '封面 URL',
    `intro`          VARCHAR(500) DEFAULT NULL COMMENT '简介',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0 连载 / 1 完本 / 2 下架',
    `audit_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待审 / 1 通过 / 2 驳回',
    `is_vip_library` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否会员书库作品：0 否 1 是',
    `chapter_price`  BIGINT       NOT NULL DEFAULT 0 COMMENT '默认单章价格（分）',
    `word_count`     BIGINT       NOT NULL DEFAULT 0 COMMENT '总字数',
    `visit_count`    BIGINT       NOT NULL DEFAULT 0 COMMENT '点击量',
    `collect_count`  BIGINT       NOT NULL DEFAULT 0 COMMENT '收藏数',
    `comment_count`  BIGINT       NOT NULL DEFAULT 0 COMMENT '评论数',
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_author_id` (`author_id`)
) ENGINE = InnoDB COMMENT = '作品表';

-- ------------------------------------------------------------
-- 5. 章节表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `book_chapter`;
CREATE TABLE `book_chapter` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id`       BIGINT UNSIGNED NOT NULL COMMENT '作品 ID',
    `title`         VARCHAR(100) NOT NULL COMMENT '章节名',
    `content`       LONGTEXT     COMMENT '正文',
    `word_count`    INT          NOT NULL DEFAULT 0 COMMENT '字数',
    `is_free`       TINYINT      NOT NULL DEFAULT 0 COMMENT '1 免费 / 0 付费',
    `price`         BIGINT       NOT NULL DEFAULT 0 COMMENT '付费价格（分）',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '章节序号',
    `audit_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待审 / 1 通过 / 2 驳回',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_sort` (`book_id`, `sort`),
    KEY `idx_book_audit` (`book_id`, `audit_status`)
) ENGINE = InnoDB COMMENT = '章节表';

-- ------------------------------------------------------------
-- 6. 书架表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `bookshelf`;
CREATE TABLE `bookshelf` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `book_id`     BIGINT UNSIGNED NOT NULL COMMENT '作品 ID',
    `is_deleted`  TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`)
) ENGINE = InnoDB COMMENT = '书架表';

-- ------------------------------------------------------------
-- 7. 阅读进度表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `reading_progress`;
CREATE TABLE `reading_progress` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `book_id`     BIGINT UNSIGNED NOT NULL COMMENT '作品 ID',
    `chapter_id`  BIGINT UNSIGNED NOT NULL COMMENT '最近阅读章节 ID',
    `progress`    TINYINT     NOT NULL DEFAULT 0 COMMENT '章内进度百分比 0~100',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`)
) ENGINE = InnoDB COMMENT = '阅读进度表';

-- ------------------------------------------------------------
-- 8. 评论表（chapter_id 为 NULL 时表示书评）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '评论人 ID',
    `book_id`     BIGINT UNSIGNED NOT NULL COMMENT '作品 ID',
    `chapter_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '章节 ID，NULL 表示书评',
    `content`     VARCHAR(500) NOT NULL COMMENT '评论内容',
    `like_count`  INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 已隐藏',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '评论表';

-- ------------------------------------------------------------
-- 9. 论坛帖子表（找书）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `forum_post`;
CREATE TABLE `forum_post` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '发帖人 ID',
    `title`         VARCHAR(100) NOT NULL COMMENT '标题',
    `content`       TEXT         COMMENT '正文',
    `reply_count`   INT          NOT NULL DEFAULT 0 COMMENT '回复数',
    `audit_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待审 / 1 通过 / 2 驳回',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 已封帖',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '论坛帖子表';

-- ------------------------------------------------------------
-- 10. 论坛回复表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `forum_reply`;
CREATE TABLE `forum_reply` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id`       BIGINT UNSIGNED NOT NULL COMMENT '帖子 ID',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '回复人 ID',
    `content`       VARCHAR(500) NOT NULL COMMENT '回复内容',
    `audit_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待审 / 1 通过 / 2 驳回',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE = InnoDB COMMENT = '论坛回复表';

-- ------------------------------------------------------------
-- 11. 充值档位表（Web 端配置）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `recharge_plan`;
CREATE TABLE `recharge_plan` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`        VARCHAR(50) NOT NULL COMMENT '档位名，如 100 元档',
    `amount`       BIGINT      NOT NULL COMMENT '充值金额（分）',
    `bonus_amount` BIGINT      NOT NULL DEFAULT 0 COMMENT '赠送金额（分）',
    `sort`         TINYINT     NOT NULL DEFAULT 0 COMMENT '排序',
    `status`       TINYINT     NOT NULL DEFAULT 0 COMMENT '0 上架 / 1 下架',
    `is_deleted`   TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = '充值档位表';

-- ------------------------------------------------------------
-- 12. VIP 套餐表（Web 端配置）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `vip_plan`;
CREATE TABLE `vip_plan` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           VARCHAR(20)  NOT NULL COMMENT '套餐名：月卡/季卡/年卡',
    `duration_days`  INT          NOT NULL COMMENT '有效天数',
    `price`          BIGINT       NOT NULL COMMENT '售价（分）',
    `discount`       TINYINT      NOT NULL DEFAULT 100 COMMENT '购书折扣，90 表示 9 折',
    `privileges`     VARCHAR(255) DEFAULT NULL COMMENT '权益文案，分号分隔',
    `sort`           TINYINT      NOT NULL DEFAULT 0 COMMENT '排序',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0 上架 / 1 下架',
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = 'VIP 套餐表';

-- ------------------------------------------------------------
-- 13. VIP 订阅记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `vip_subscription`;
CREATE TABLE `vip_subscription` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`    VARCHAR(32)  NOT NULL COMMENT '订单号',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `plan_id`     BIGINT UNSIGNED NOT NULL COMMENT '套餐 ID',
    `amount`      BIGINT       NOT NULL COMMENT '实付金额（分）',
    `start_time`  DATETIME     NOT NULL COMMENT '生效时间',
    `expire_time` DATETIME     NOT NULL COMMENT '到期时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = 'VIP 订阅记录表';

-- ------------------------------------------------------------
-- 14. 余额流水表（所有资金变动的唯一记账入口）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `balance_flow`;
CREATE TABLE `balance_flow` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `flow_no`        VARCHAR(32)  NOT NULL COMMENT '流水号',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '用户 ID（作者收益记在作者账户下）',
    `type`           TINYINT      NOT NULL COMMENT '1 充值 / 2 购章支出 / 3 打赏支出 / 4 打赏收益 / 5 购章分成收益 / 6 管理员调整',
    `amount`         BIGINT       NOT NULL COMMENT '变动金额（恒为正数，方向由 type 决定）',
    `balance_after`  BIGINT       NOT NULL COMMENT '变动后余额（分），对账用',
    `ref_type`       TINYINT      DEFAULT NULL COMMENT '关联对象类型：1 档位 / 2 章节 / 3 作品 / 4 套餐',
    `ref_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '关联对象 ID',
    `remark`         VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_no` (`flow_no`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE = InnoDB COMMENT = '余额流水表';

-- ------------------------------------------------------------
-- 15. 章节购买记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `chapter_purchase`;
CREATE TABLE `chapter_purchase` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '购买人 ID',
    `chapter_id`    BIGINT UNSIGNED NOT NULL COMMENT '章节 ID',
    `book_id`       BIGINT UNSIGNED NOT NULL COMMENT '作品 ID（冗余，便于按书查询）',
    `amount`        BIGINT       NOT NULL COMMENT '实付金额（分，已含 VIP 折扣）',
    `author_income` BIGINT       NOT NULL DEFAULT 0 COMMENT '作者分成（分）',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '购买时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_chapter` (`user_id`, `chapter_id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE = InnoDB COMMENT = '章节购买记录表';

-- ------------------------------------------------------------
-- 16. 打赏记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `tip_record`;
CREATE TABLE `tip_record` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tip_no`      VARCHAR(32)  NOT NULL COMMENT '打赏单号',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '打赏人 ID',
    `book_id`     BIGINT UNSIGNED NOT NULL COMMENT '被打赏作品 ID',
    `amount`      BIGINT       NOT NULL COMMENT '金额（分）',
    `message`     VARCHAR(100) DEFAULT NULL COMMENT '附言',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打赏时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tip_no` (`tip_no`),
    KEY `idx_book_id` (`book_id`)
) ENGINE = InnoDB COMMENT = '打赏记录表';

-- ------------------------------------------------------------
-- 17. 管理员表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `role`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 超管 / 2 运营 / 3 审核员',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 停用',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT = '管理员表';

-- ------------------------------------------------------------
-- 18. 审核记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `audit_record`;
CREATE TABLE `audit_record` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `admin_id`    BIGINT UNSIGNED NOT NULL COMMENT '审核人 ID',
    `target_type` TINYINT      NOT NULL COMMENT '1 章节 / 2 帖子 / 3 回复 / 4 评论',
    `target_id`   BIGINT UNSIGNED NOT NULL COMMENT '审核对象 ID',
    `result`      TINYINT      NOT NULL COMMENT '1 通过 / 2 驳回',
    `reason`      VARCHAR(200) DEFAULT NULL COMMENT '驳回理由',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`)
) ENGINE = InnoDB COMMENT = '审核记录表';

-- =============================================================
-- 种子数据
-- =============================================================

-- 测试读者账号（手机号 13926356630 / 13800138000，密码均为 123456）
INSERT INTO `user` (`username`, `phone`, `password`, `nickname`, `balance`, `is_author`) VALUES
('reader01', '13926356630', '$2b$12$77QyJU.aL4MddtEEtO83GuEALLkZc.8HlZMhJpXZSZzXSsO2hD1g.', '书虫小寻', 100000, 0),
('author01', '13800138000', '$2b$12$77QyJU.aL4MddtEEtO83GuEALLkZc.8HlZMhJpXZSZzXSsO2hD1g.', '寻页作者', 0, 1);

-- 管理员（admin / admin123）
INSERT INTO `admin_user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2b$12$V8ujFs8N.o.4LOCkfwvyUeRr2pLDaW2rJkNJWtspSjddYxEkFPiFq', '超级管理员', 1);

-- 作品分类
INSERT INTO `book_category` (`name`, `sort`) VALUES
('玄幻', 1), ('都市', 2), ('科幻', 3), ('悬疑', 4), ('历史', 5), ('言情', 6), ('武侠', 7), ('奇幻', 8);

-- 充值档位
INSERT INTO `recharge_plan` (`title`, `amount`, `bonus_amount`, `sort`) VALUES
('30 元档', 3000, 0, 1),
('50 元档', 5000, 500, 2),
('100 元档', 10000, 1500, 3),
('200 元档', 20000, 4000, 4);

-- VIP 套餐
INSERT INTO `vip_plan` (`name`, `duration_days`, `price`, `discount`, `privileges`, `sort`) VALUES
('月卡', 30, 1800, 90, '会员书库免费读；购章 9 折；免广告；专属标识', 1),
('季卡', 90, 4800, 85, '会员书库免费读；购章 8.5 折；免广告；专属标识', 2),
('年卡', 365, 15800, 80, '会员书库免费读；购章 8 折；免广告；专属标识；会员主题', 3);

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

-- M3 additive tables
-- M3增量：只新增表。可重复执行；金额单位均为分。
CREATE TABLE IF NOT EXISTS recharge_order (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 order_no VARCHAR(32) NOT NULL UNIQUE,
 user_id BIGINT NOT NULL, plan_id BIGINT NOT NULL, request_id VARCHAR(64) NOT NULL,
 amount BIGINT NOT NULL, bonus_amount BIGINT NOT NULL,
 status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已入账 2已关闭',
 expires_at DATETIME NOT NULL, paid_at DATETIME NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_recharge_request(user_id,request_id)
);
CREATE TABLE IF NOT EXISTS forum_like (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, target_type TINYINT NOT NULL COMMENT '1帖子 2回复', target_id BIGINT NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_forum_like(user_id,target_type,target_id),
 KEY idx_forum_like_target(target_type,target_id)
);
CREATE TABLE IF NOT EXISTS `recharge_plan` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`        VARCHAR(50) NOT NULL COMMENT '档位名，如 100 元档',
    `amount`       BIGINT      NOT NULL COMMENT '充值金额（分）',
    `bonus_amount` BIGINT      NOT NULL DEFAULT 0 COMMENT '赠送金额（分）',
    `sort`         TINYINT     NOT NULL DEFAULT 0 COMMENT '排序',
    `status`       TINYINT     NOT NULL DEFAULT 0 COMMENT '0 上架 / 1 下架',
    `is_deleted`   TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = '充值档位表';
CREATE TABLE IF NOT EXISTS `balance_flow` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `flow_no`        VARCHAR(32)  NOT NULL COMMENT '流水号',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '用户 ID（作者收益记在作者账户下）',
    `type`           TINYINT      NOT NULL COMMENT '1 充值 / 2 购章支出 / 3 打赏支出 / 4 打赏收益 / 5 购章分成收益 / 6 管理员调整',
    `amount`         BIGINT       NOT NULL COMMENT '变动金额（恒为正数，方向由 type 决定）',
    `balance_after`  BIGINT       NOT NULL COMMENT '变动后余额（分），对账用',
    `ref_type`       TINYINT      DEFAULT NULL COMMENT '关联对象类型：1 档位 / 2 章节 / 3 作品 / 4 套餐',
    `ref_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '关联对象 ID',
    `remark`         VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_no` (`flow_no`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE = InnoDB COMMENT = '余额流水表';
CREATE TABLE IF NOT EXISTS `chapter_purchase` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '购买人 ID',
    `chapter_id`    BIGINT UNSIGNED NOT NULL COMMENT '章节 ID',
    `book_id`       BIGINT UNSIGNED NOT NULL COMMENT '作品 ID（冗余，便于按书查询）',
    `amount`        BIGINT       NOT NULL COMMENT '实付金额（分，已含 VIP 折扣）',
    `author_income` BIGINT       NOT NULL DEFAULT 0 COMMENT '作者分成（分）',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '购买时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_chapter` (`user_id`, `chapter_id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE = InnoDB COMMENT = '章节购买记录表';
CREATE TABLE IF NOT EXISTS `tip_record` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tip_no`      VARCHAR(32)  NOT NULL COMMENT '打赏单号',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '打赏人 ID',
    `book_id`     BIGINT UNSIGNED NOT NULL COMMENT '被打赏作品 ID',
    `amount`      BIGINT       NOT NULL COMMENT '金额（分）',
    `message`     VARCHAR(100) DEFAULT NULL COMMENT '附言',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打赏时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tip_no` (`tip_no`),
    KEY `idx_book_id` (`book_id`)
) ENGINE = InnoDB COMMENT = '打赏记录表';
