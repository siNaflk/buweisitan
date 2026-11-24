-- 仅在完全没有可用充值档位时补充演示档位，不覆盖已有运营配置。
INSERT INTO recharge_plan(title,amount,bonus_amount,sort,status,is_deleted)
SELECT p.title,p.amount,p.bonus_amount,p.sort,0,0 FROM (
 SELECT '轻享10元' AS title,1000 AS amount,0 AS bonus_amount,1 AS sort
 UNION ALL SELECT '畅读30元',3000,300,2
 UNION ALL SELECT '悦读100元',10000,1500,3
) p WHERE NOT EXISTS (SELECT 1 FROM recharge_plan WHERE status=0 AND is_deleted=0);
