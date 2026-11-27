-- M4增量：可重复执行，仅新增表；不覆盖已有VIP套餐或订阅。
CREATE TABLE IF NOT EXISTS vip_subscription_detail (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, subscription_id BIGINT NOT NULL UNIQUE,
 user_id BIGINT NOT NULL, request_id VARCHAR(64) NOT NULL, plan_name VARCHAR(50) NOT NULL,
 duration_days INT NOT NULL, discount INT NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(user_id,request_id)
);
CREATE TABLE IF NOT EXISTS mq_outbox (
 id VARCHAR(32) NOT NULL PRIMARY KEY, event_key VARCHAR(160) NOT NULL UNIQUE,
 event_type VARCHAR(32) NOT NULL, user_id BIGINT NOT NULL, ref_id BIGINT NOT NULL,
 deliver_at DATETIME NULL, status INT NOT NULL DEFAULT 0, attempts INT NOT NULL DEFAULT 0,
 next_attempt_at DATETIME NOT NULL, consumed_at DATETIME NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS user_notification (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, event_id VARCHAR(32) NOT NULL UNIQUE,
 user_id BIGINT NOT NULL, title VARCHAR(100) NOT NULL, content VARCHAR(500) NOT NULL,
 is_read INT NOT NULL DEFAULT 0,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS `vip_plan` (
    `id`             BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
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
);
CREATE TABLE IF NOT EXISTS `vip_subscription` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`    VARCHAR(32)  NOT NULL COMMENT '订单号',
    `user_id`     BIGINT NOT NULL COMMENT '用户 ID',
    `plan_id`     BIGINT NOT NULL COMMENT '套餐 ID',
    `amount`      BIGINT       NOT NULL COMMENT '实付金额（分）',
    `start_time`  DATETIME     NOT NULL COMMENT '生效时间',
    `expire_time` DATETIME     NOT NULL COMMENT '到期时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE (`order_no`)
);
