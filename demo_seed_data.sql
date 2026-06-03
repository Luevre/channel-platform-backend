-- ============================================================
-- 渠道投放平台 — 演示 Demo 数据（快手 + 京东金融）
-- 执行前请确认已选中数据库：USE channel_platform;
-- ============================================================

-- ============================================================
-- 修复说明：conv_node.conv_type 有唯一约束，使用 100+ 避免冲突
-- 若 placement_param 已执行成功，跳过第一节直接从第二节开始
-- ============================================================

-- ============================================================
-- 一、投放参数（placement_param）
-- ============================================================

INSERT INTO placement_param
    (param_name, param_key, param_value, channel_code, status, category,
     param_usage, manager, deleted, audit_status, created_at, updated_at)
VALUES
-- 快手专属参数
('快手渠道标识',   'kuaishou_cid',  'kuaishou_001', 'kuaishou', 1, '场景参数',
 '标识快手信息流广告的渠道来源，用于归因和效果追踪',
 'admin', 0, 1, NOW(), NOW()),
('快手活动编号',   'kuaishou_act',  '',             'kuaishou', 1, '活动参数',
 '快手平台专属活动追踪参数，统计活动页面转化数据（开户/签约等）',
 'admin', 0, 1, NOW(), NOW()),
('快手推广计划ID', 'ks_plan_id',    '',             'kuaishou', 1, '场景参数',
 '快手广告推广计划唯一编号，用于区分不同计划的转化数据',
 'admin', 0, 1, NOW(), NOW()),

-- 京东金融专属参数
('京东渠道编码',   'jd_channel_code', 'jd_finance',  'jingdong', 1, '场景参数',
 '标识京东金融渠道的广告来源，用于投放效果统计',
 'admin', 0, 1, NOW(), NOW()),
('京东活动ID',     'jd_act_id',       '',            'jingdong', 1, '活动参数',
 '京东金融平台专属活动追踪参数，统计活动页面新用户转化数量',
 'admin', 0, 1, NOW(), NOW());


-- ============================================================
-- 二、回传节点（conv_node）
-- ============================================================

INSERT INTO conv_node
    (node_name, conv_type, event_category, event_desc, creator_name, deleted, created_at, updated_at)
VALUES
-- 快手节点（conv_type 用 101~105 避免唯一键冲突）
('快手APP下载完成',   101, '下载',
 '用户通过快手广告完成金证通APP下载并安装，触发下载回传', 'admin', 0, NOW(), NOW()),
('快手新用户注册',    102, '注册',
 '通过快手广告点击链接后，完成金证通新用户注册，触发注册回传', 'admin', 0, NOW(), NOW()),
('快手账户开通',      103, '开户',
 '通过快手广告完成证券账户开通，触发开户回传', 'admin', 0, NOW(), NOW()),

-- 京东金融节点
('京东金融用户注册',  111, '注册',
 '通过京东金融广告位点击，完成新用户注册，触发注册转化回传', 'admin', 0, NOW(), NOW()),
('京东金融账户开通',  112, '开户',
 '通过京东金融渠道完成证券账户开通，触发开户转化回传', 'admin', 0, NOW(), NOW());


-- ============================================================
-- 三、回传策略（conv_strategy）
-- 用变量绑定 ID，再关联节点
-- ============================================================

-- 快手-下载回传策略
INSERT INTO conv_strategy
    (strategy_name, channel, channel_code, description, audit_status,
     strategy_rule, callback_ratio, node_overview, creator_name, deleted, created_at, updated_at)
VALUES
('快手-下载回传策略', '快手', 'kuaishou',
 '快手信息流广告下载转化回传，追踪APP下载安装',
 1, '下载回传模型', 100, '快手APP下载完成', 'admin', 0, NOW(), NOW());

SET @ks_strategy_id = LAST_INSERT_ID();

-- 快手-注册回传策略
INSERT INTO conv_strategy
    (strategy_name, channel, channel_code, description, audit_status,
     strategy_rule, callback_ratio, node_overview, creator_name, deleted, created_at, updated_at)
VALUES
('快手-注册回传策略', '快手', 'kuaishou',
 '快手信息流广告注册转化回传，追踪新用户注册',
 1, '自定义模型', 100, '快手新用户注册 → 快手账户开通', 'admin', 0, NOW(), NOW());

SET @ks_register_strategy_id = LAST_INSERT_ID();

-- 京东金融-注册回传策略
INSERT INTO conv_strategy
    (strategy_name, channel, channel_code, description, audit_status,
     strategy_rule, callback_ratio, node_overview, creator_name, deleted, created_at, updated_at)
VALUES
('京东金融-注册回传策略', '京东金融', 'jingdong',
 '京东金融渠道新用户注册及开户转化回传',
 1, '下载留存模型', 80, '京东金融用户注册 → 京东金融账户开通', 'admin', 0, NOW(), NOW());

SET @jd_strategy_id = LAST_INSERT_ID();


-- ============================================================
-- 四、策略-节点关联（conv_strategy_node）
-- ============================================================

-- 获取刚插入的节点 ID
SET @ks_dl_node   = (SELECT id FROM conv_node WHERE node_name = '快手APP下载完成'  AND deleted = 0 ORDER BY id DESC LIMIT 1);
SET @ks_reg_node  = (SELECT id FROM conv_node WHERE node_name = '快手新用户注册'   AND deleted = 0 ORDER BY id DESC LIMIT 1);
SET @ks_open_node = (SELECT id FROM conv_node WHERE node_name = '快手账户开通'     AND deleted = 0 ORDER BY id DESC LIMIT 1);
SET @jd_reg_node  = (SELECT id FROM conv_node WHERE node_name = '京东金融用户注册' AND deleted = 0 ORDER BY id DESC LIMIT 1);
SET @jd_open_node = (SELECT id FROM conv_node WHERE node_name = '京东金融账户开通' AND deleted = 0 ORDER BY id DESC LIMIT 1);

INSERT INTO conv_strategy_node (strategy_id, node_id, sort_order, ad_param)
VALUES
-- 快手下载策略 关联 下载节点
(@ks_strategy_id,         @ks_dl_node,   1, 'AD_DOWNLOAD'),
-- 快手注册策略 关联 注册 + 开户节点
(@ks_register_strategy_id, @ks_reg_node,  1, 'AD_REGISTER'),
(@ks_register_strategy_id, @ks_open_node, 2, 'AD_OPEN_ACCOUNT'),
-- 京东金融策略 关联 注册 + 开户节点
(@jd_strategy_id,          @jd_reg_node,  1, NULL),
(@jd_strategy_id,          @jd_open_node, 2, NULL);


-- ============================================================
-- 五、投放链接（delivery_link）
-- ============================================================

INSERT INTO delivery_link
    (link_name, channel, channel_code, content, promo_mode,
     origin_url, link_status, creator_name, deleted, created_at, updated_at)
VALUES
-- 快手
('快手信息流-金证通APP下载',
 '快手', 'kuaishou', 'AI投顾产品', '网销线上推广',
 'https://a.app.qq.com/o/simple.jsp?pkgname=com.sinolink.droid',
 '生效中', 'admin', 0, NOW(), NOW()),

('快手开屏广告-AI选好股',
 '快手', 'kuaishou', 'AI投顾产品', '网销线上推广',
 'https://www.jzlcapp.com/h5/activity/ai-stock?source=kuaishou',
 '生效中', 'admin', 0, NOW(), NOW()),

-- 京东金融
('京东金融搜索入口-AI选好股',
 '京东金融', 'jingdong', 'AI投顾产品', '网销线上推广',
 'https://www.jzlcapp.com/h5/activity/ai-stock?source=jingdong',
 '生效中', 'admin', 0, NOW(), NOW()),

('京东金融首页Banner-财经课',
 '京东金融', 'jingdong', '财经课', '网销线上推广',
 'https://www.jzlcapp.com/h5/course/finance?source=jingdong_banner',
 '已过期', 'admin', 0, NOW(), NOW());


-- ============================================================
-- 完成提示
-- ============================================================
SELECT '快手 + 京东金融演示数据写入完成' AS result;
SELECT '投放参数' AS type, COUNT(*) AS inserted FROM placement_param WHERE channel_code IN ('kuaishou','jingdong') AND deleted = 0
UNION ALL
SELECT '回传节点', COUNT(*) FROM conv_node WHERE node_name LIKE '%快手%' OR node_name LIKE '%京东金融%' AND deleted = 0
UNION ALL
SELECT '回传策略', COUNT(*) FROM conv_strategy WHERE channel_code IN ('kuaishou','jingdong') AND deleted = 0
UNION ALL
SELECT '投放链接', COUNT(*) FROM delivery_link WHERE channel_code IN ('kuaishou','jingdong') AND deleted = 0;
