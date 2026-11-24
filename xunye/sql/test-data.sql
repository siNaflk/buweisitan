-- =============================================================
-- 寻页（XunYe）M1 演示测试数据：6 部作品 + 每部 4 章
-- 作者统一为 author01（user.id = 2），章节前 2 章免费、后 2 章付费（400 分）
-- =============================================================
USE `xunye`;

INSERT INTO `book` (`title`, `author_id`, `author_name`, `category_id`, `cover_url`, `intro`, `status`, `audit_status`, `is_vip_library`, `chapter_price`, `word_count`, `visit_count`, `collect_count`)
VALUES
('遗迹纪元', 2, '寻页作者', 1, NULL, '灵气复苏的时代，少年在废弃遗迹中捡到一枚刻着星图的古玉，从此踏上追寻上古文明之路。', 0, 1, 1, 400, 82000, 15600, 320),
('重生之都市匠心', 2, '寻页作者', 2, NULL, '建筑设计师叶沉舟重生回大学时代，这一世他要把每一条街都修成艺术品。', 0, 1, 0, 400, 76500, 23400, 518),
('星海拓荒者', 2, '寻页作者', 3, NULL, '曲率引擎首次点火，第一批拓荒者离开太阳系，却发现深空中早有人在等候。', 1, 1, 0, 400, 98000, 8900, 201),
('雾镇谜案', 2, '寻页作者', 4, NULL, '常年被海雾笼罩的小镇连续出现无人目击的失踪案，法医林晚在死者指甲缝里找到了同一枚鱼钩。', 0, 1, 0, 400, 64000, 11200, 176),
('大明闲吏', 2, '寻页作者', 5, NULL, '一个户部九品小吏，只想按账册过日子，偏偏每一笔账后头都站着一个不该惹的人。', 0, 1, 0, 400, 71200, 6700, 98),
('春风渡不渡', 2, '寻页作者', 6, NULL, '渡口客栈的老板娘守着一条据说能渡到十年前的旧船，等一个每年春天都回来的旅人。', 1, 1, 0, 400, 58000, 4300, 87);

-- 章节内容样例（每章同一段演示正文）
SET @demo = '晨光从残缺的穹顶斜切进来，照亮浮动的尘埃。他屏住呼吸，指尖的旧玉忽然微微发烫，像有什么东西在玉纹深处醒了过来。石壁上的星图一一亮起，又一一熄灭，最后只剩下北斗第七星的位置还残着一点微光。他知道，从这一刻起，脚下的路已经和从前不一样了。\n远处的钟声撞碎了薄雾，归鸟掠过檐角。他收起玉，把断剑背回身后，朝着光走来的方向，一步、一步，走了下去。';

INSERT INTO `book_chapter` (`book_id`, `title`, `content`, `word_count`, `is_free`, `price`, `sort`, `audit_status`)
SELECT b.id,
       CONCAT('第', seq.s, '章 ', seq.name),
       @demo,
       820,
       CASE WHEN seq.s <= 2 THEN 1 ELSE 0 END,
       CASE WHEN seq.s <= 2 THEN 0 ELSE 400 END,
       seq.s,
       1
FROM book b
JOIN (
    SELECT 1 AS s, '初见' AS name UNION ALL
    SELECT 2, '星图' UNION ALL
    SELECT 3, '入局' UNION ALL
    SELECT 4, '抉择'
) seq ON seq.s <= 4;

-- 校验
SELECT COUNT(*) AS books FROM book;
SELECT COUNT(*) AS chapters FROM book_chapter;
