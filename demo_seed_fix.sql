-- ============================================================
-- Demo 补充数据（回传节点 / 策略 / 链接）
-- placement_param 已成功插入，此脚本从回传节点开始
-- USE channel_platform; 后直接运行全文
-- ============================================================

USE channel_platform;

-- ============================================================
-- 二、回传节点（conv_node）  conv_type 使用 101~112
-- ============================================================

INSERT INTO conv_node
    (node_name, conv_type, event_category, event_desc, creator_name, deleted, created_at, updated_at)
VALUES
('快手APP下载完成',   101, '下载',
 '用户通过快手广告完成金证通APP下载并安装，触发下载回传', 'admin', 0, NOW(), NOW()),
('快手新用户注册',    102, '注册',
 '通过快手广告点击链接后，完成金证通新用户注册，触发注册回传', 'admin', 0, NOW(), NOW()),
('快手账户开通',      103, '开户',
 '通过快手广告完成证券账户开通，触发开户回传', 'admin', 0, NOW(), NOW()),
('京东金融用户注册',  111, '注册',
 '通过京东金融广告位点击，完成新用户注册，触发注册转化回传', 'admin', 0, NOW(), NOW()),
('京东金融账户开通',  112, '开户',
 '通过京东金融渠道完成证券账户开通，触发开户转化回传', 'admin', 0, NOW(), NOW());


-- ============================================================
-- 三、回传策略（conv_strategy）
-- ============================================================

INSERT INTO conv_strategy
    (strategy_name, channel, channel_code, description, audit_status,
     strategy_rule, callback_ratio, node_overview, creator_name, deleted, created_at, updated_at)
VALUES
('快手-下载回传策略', '快手', 'kuaishou',
 '快手信息流广告下载转化回传，追踪APP下载安装',
 1, '下载回传模型', 100, '快手APP下载完成', 'admin', 0, NOW(), NOW());

SET @ks_dl_strategy = LAST_INSERT_ID();

INSERT INTO conv_strategy
    (strategy_name, channel, channel_code, description, audit_status,
     strategy_rule, callback_ratio, node_overview, creator_name, deleted, created_at, updated_at)
VALUES
('快手-注册回传策略', '快手', 'kuaishou',
 '快手信息流广告注册转化回传，追踪新用户注册与开户',
 1, '自定义模型', 100, '快手新用户注册 → 快手账户开通', 'admin', 0, NOW(), NOW());

SET @ks_reg_strategy = LAST_INSERT_ID();

INSERT INTO conv_strategy
    (strategy_name, channel, channel_code, description, audit_status,
     strategy_rule, callback_ratio, node_overview, creator_name, deleted, created_at, updated_at)
VALUES
('京东金融-注册回传策略', '京东金融', 'jingdong',
 '京东金融渠道新用户注册及开户转化回传',
 1, '下载留存模型', 80, '京东金融用户注册 → 京东金融账户开通', 'admin', 0, NOW(), NOW());

SET @jd_strategy = LAST_INSERT_ID();


-- ============================================================
-- 四、策略-节点关联（conv_strategy_node）
-- ============================================================

SET @ks_dl_node   = (SELECT id FROM conv_node WHERE conv_type = 101 AND deleted = 0 LIMIT 1);
SET @ks_reg_node  = (SELECT id FROM conv_node WHERE conv_type = 102 AND deleted = 0 LIMIT 1);
SET @ks_open_node = (SELECT id FROM conv_node WHERE conv_type = 103 AND deleted = 0 LIMIT 1);
SET @jd_reg_node  = (SELECT id FROM conv_node WHERE conv_type = 111 AND deleted = 0 LIMIT 1);
SET @jd_open_node = (SELECT id FROM conv_node WHERE conv_type = 112 AND deleted = 0 LIMIT 1);

INSERT INTO conv_strategy_node (strategy_id, node_id, sort_order, ad_param)
VALUES
(@ks_dl_strategy,  @ks_dl_node,   1, 'AD_DOWNLOAD'),
(@ks_reg_strategy, @ks_reg_node,  1, 'AD_REGISTER'),
(@ks_reg_strategy, @ks_open_node, 2, 'AD_OPEN_ACCOUNT'),
(@jd_strategy,     @jd_reg_node,  1, NULL),
(@jd_strategy,     @jd_open_node, 2, NULL);


-- ============================================================
-- 五、投放链接（delivery_link）
-- ============================================================

INSERT INTO delivery_link
    (link_name, channel, channel_code, content, promo_mode,
     origin_url, link_status, creator_name, deleted, created_at, updated_at)
VALUES
('快手信息流-金证通APP下载',
 '快手', 'kuaishou', 'AI投顾产品', '网销线上推广',
 'https://a.app.qq.com/o/simple.jsp?pkgname=com.sinolink.droid',
 '生效中', 'admin', 0, NOW(), NOW()),

('快手开屏广告-AI选好股',
 '快手', 'kuaishou', 'AI投顾产品', '网销线上推广',
 'https://www.jzlcapp.com/h5/activity/ai-stock?source=kuaishou',
 '生效中', 'admin', 0, NOW(), NOW()),

('京东金融搜索入口-AI选好股',
 '京东金融', 'jingdong', 'AI投顾产品', '网销线上推广',
 'https://www.jzlcapp.com/h5/activity/ai-stock?source=jingdong',
 '生效中', 'admin', 0, NOW(), NOW()),

('京东金融首页Banner-财经课',
 '京东金融', 'jingdong', '财经课', '网销线上推广',
 'https://www.jzlcapp.com/h5/course/finance?source=jingdong_banner',
 '已过期', 'admin', 0, NOW(), NOW());


-- ============================================================
-- 验证
-- ============================================================
SELECT '回传节点' AS type, COUNT(*) AS cnt FROM conv_node   WHERE conv_type BETWEEN 101 AND 112
UNION ALL
SELECT '回传策略', COUNT(*) FROM conv_strategy WHERE channel_code IN ('kuaishou','jingdong') AND deleted = 0
UNION ALL
SELECT '投放链接', COUNT(*) FROM delivery_link WHERE channel_code IN ('kuaishou','jingdong') AND deleted = 0;
