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
