-- 无任何上架套餐时才提供演示套餐，不覆盖用户已配置价格。
INSERT INTO vip_plan(name,duration_days,price,discount,privileges,sort,status,is_deleted)
SELECT p.name,p.days,p.price,p.discount,'会员专属作品畅读;普通作品全部章节畅读',p.sort,0,0 FROM (
 SELECT '月卡' name,30 days,1500 price,90 discount,1 sort
 UNION ALL SELECT '季卡',90,3900,85,2
 UNION ALL SELECT '年卡',365,12800,80,3
) p WHERE NOT EXISTS(SELECT 1 FROM vip_plan WHERE status=0 AND is_deleted=0);
