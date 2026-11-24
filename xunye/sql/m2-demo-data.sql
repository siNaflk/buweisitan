-- 可选演示数据，不更改现有账号或密码。只使用已有作者（需先完成M1初始化）。
USE xunye;
INSERT INTO forum_post(user_id,title,content,reply_count,audit_status,status)
SELECT u.id,'【M2演示】寻找星海题材的小说','这是明确标注的演示帖子：想找一本探索宇宙、人物成长扎实的小说，欢迎分享推荐。',0,1,0
FROM `user` u WHERE u.phone='13800138000' AND u.is_deleted=0
AND NOT EXISTS(SELECT 1 FROM forum_post WHERE title='【M2演示】寻找星海题材的小说' AND is_deleted=0);
INSERT INTO forum_reply(post_id,user_id,content,audit_status)
SELECT p.id,p.user_id,'【M2演示回复】可以从书城的科幻分类开始看看。',1
FROM forum_post p WHERE p.title='【M2演示】寻找星海题材的小说' AND p.is_deleted=0
AND NOT EXISTS(SELECT 1 FROM forum_reply r WHERE r.post_id=p.id AND r.content='【M2演示回复】可以从书城的科幻分类开始看看。' AND r.is_deleted=0);
-- M6管理端上线前，审核演示时在DataGrip按具体ID执行（先SELECT确认，不要批量通过所有帖子）：
-- UPDATE forum_post SET audit_status=1, update_time=NOW() WHERE id=<已确认的帖子ID> AND audit_status=0;
-- UPDATE forum_reply SET audit_status=1, update_time=NOW() WHERE id=<已确认的回复ID> AND audit_status=0;
