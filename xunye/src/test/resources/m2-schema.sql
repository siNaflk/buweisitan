DROP ALL OBJECTS;
CREATE TABLE `user` (`id`              BIGINT NOT NULL AUTO_INCREMENT,
    `username`        VARCHAR(50)  NOT NULL,
    `phone`           VARCHAR(20)  NOT NULL,
    `password`        VARCHAR(100) NOT NULL,
    `nickname`        VARCHAR(50)  DEFAULT NULL,
    `avatar`          VARCHAR(255) DEFAULT NULL,
    `balance`         BIGINT       NOT NULL DEFAULT 0,
    `is_author`       TINYINT      NOT NULL DEFAULT 0,
    `vip_expire_time` DATETIME     DEFAULT NULL,
    `status`          TINYINT      NOT NULL DEFAULT 0,
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE (`phone`));
CREATE TABLE `book` (`id`             BIGINT NOT NULL AUTO_INCREMENT,
    `title`          VARCHAR(100) NOT NULL,
    `author_id`      BIGINT NOT NULL,
    `author_name`    VARCHAR(50)  DEFAULT NULL,
    `category_id`    BIGINT NOT NULL,
    `cover_url`      VARCHAR(255) DEFAULT NULL,
    `intro`          VARCHAR(500) DEFAULT NULL,
    `status`         TINYINT      NOT NULL DEFAULT 0,
    `audit_status`   TINYINT      NOT NULL DEFAULT 0,
    `is_vip_library` TINYINT      NOT NULL DEFAULT 0,
    `chapter_price`  BIGINT       NOT NULL DEFAULT 0,
    `word_count`     BIGINT       NOT NULL DEFAULT 0,
    `visit_count`    BIGINT       NOT NULL DEFAULT 0,
    `collect_count`  BIGINT       NOT NULL DEFAULT 0,
    `comment_count`  BIGINT       NOT NULL DEFAULT 0,
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`));
CREATE TABLE `book_chapter` (`id`            BIGINT NOT NULL AUTO_INCREMENT,
    `book_id`       BIGINT NOT NULL,
    `title`         VARCHAR(100) NOT NULL,
    `content`       LONGTEXT    ,
    `word_count`    INT          NOT NULL DEFAULT 0,
    `is_free`       TINYINT      NOT NULL DEFAULT 0,
    `price`         BIGINT       NOT NULL DEFAULT 0,
    `sort`          INT          NOT NULL DEFAULT 0,
    `audit_status`  TINYINT      NOT NULL DEFAULT 0,
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE (`book_id`, `sort`));
CREATE TABLE `comment` (`id`          BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL,
    `book_id`     BIGINT NOT NULL,
    `chapter_id`  BIGINT DEFAULT NULL,
    `content`     VARCHAR(500) NOT NULL,
    `like_count`  INT          NOT NULL DEFAULT 0,
    `status`      TINYINT      NOT NULL DEFAULT 0,
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`));
CREATE TABLE `forum_post` (`id`            BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT NOT NULL,
    `title`         VARCHAR(100) NOT NULL,
    `content`       TEXT        ,
    `reply_count`   INT          NOT NULL DEFAULT 0,
    `audit_status`  TINYINT      NOT NULL DEFAULT 0,
    `status`        TINYINT      NOT NULL DEFAULT 0,
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`));
CREATE TABLE `forum_reply` (`id`            BIGINT NOT NULL AUTO_INCREMENT,
    `post_id`       BIGINT NOT NULL,
    `user_id`       BIGINT NOT NULL,
    `content`       VARCHAR(500) NOT NULL,
    `audit_status`  TINYINT      NOT NULL DEFAULT 0,
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`));
CREATE TABLE `bookshelf` (`id`          BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL,
    `book_id`     BIGINT NOT NULL,
    `is_deleted`  TINYINT     NOT NULL DEFAULT 0,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE (`user_id`, `book_id`));
CREATE TABLE `reading_progress` (`id`          BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL,
    `book_id`     BIGINT NOT NULL,
    `chapter_id`  BIGINT NOT NULL,
    `progress`    TINYINT     NOT NULL DEFAULT 0,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE (`user_id`, `book_id`));
CREATE TABLE comment_like(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,comment_id BIGINT, UNIQUE(user_id,comment_id));
INSERT INTO book(id,title,author_id,author_name,category_id,status,audit_status,word_count) VALUES(1,'测试书',1,'作者',1,0,1,100),(2,'另一作品',1,'作者',1,0,1,100);
INSERT INTO book_chapter(id,book_id,title,content,word_count,is_free,price,sort,audit_status) VALUES(1,1,'第一章','正文',10,1,0,1,1),(2,2,'另一书章节','正文',10,1,0,1,1);

CREATE TABLE forum_like(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,target_type INT NOT NULL,target_id BIGINT NOT NULL,UNIQUE(user_id,target_type,target_id));

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
