-- =============================================
-- 渠道投放管理平台 数据库初始化脚本
-- Database: channel_platform
-- =============================================

CREATE DATABASE IF NOT EXISTS channel_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE channel_platform;

-- ----------------------------
-- 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`    VARCHAR(64) NOT NULL COMMENT '用户名',
  `password`    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt）',
  `real_name`   VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
  `email`       VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `role`        VARCHAR(32)  NOT NULL DEFAULT 'OPERATOR' COMMENT '角色: ADMIN/OPERATOR/VIEWER',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=启用 0=禁用',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 默认管理员 (密码: admin123)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6DcYa', '管理员', 'ADMIN'),
('吴小青', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6DcYa', '吴小青', 'OPERATOR');

-- ----------------------------
-- 回传节点表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `conv_node` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `node_name`     VARCHAR(64)  NOT NULL COMMENT '节点名',
  `conv_type`     INT          NOT NULL COMMENT 'convType值',
  `event_category` VARCHAR(32) NOT NULL COMMENT '节点事件分类',
  `event_desc`    VARCHAR(256) DEFAULT NULL COMMENT '节点事件说明',
  `event_judge`   VARCHAR(64)  DEFAULT NULL COMMENT '节点事件判断',
  `creator_id`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `creator_name`  VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conv_type` (`conv_type`),
  KEY `idx_event_category` (`event_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回传节点表';

-- 初始化回传节点数据（对应截图）
INSERT INTO `conv_node` (`node_name`, `conv_type`, `event_category`, `event_desc`, `event_judge`, `creator_name`) VALUES
('APP下载',       0, '下载开户节点', '客户端上报启动埋点，不区分首次与次日，取合并判断。', '客户端finder',    '吴小青'),
('首次APP启动',   1, '下载开户节点', '客户端上报启动埋点，不区分首次与次日，取合并判断。', '客户端finder',    '吴小青'),
('次日留存APP启动', 2, '下载开户节点', '客户端上报启动埋点，不区分首次与次日，取合并判断。', '客户端finder',    '吴小青'),
('签署隐私协议',  3, '下载开户节点', '签署隐私协议后触发下一个节点。',                   '客户端finder',    '吴小青'),
('用户注册',      4, '下载开户节点', '注册成功回传。',                                   'H5（防方）finder', '吴小青'),
('预约开户',      5, '下载开户节点', '预约开户成功回传。',                               'H5（防方）finder', '吴小青'),
('开户成功',      6, '下载开户节点', '数仓业务表识别判断。',                             '数仓业务表识别判断', '吴小青'),
('参加活动',      7, '活动节点',     '活动参与回传。',                                   '数仓业务表识别判断', '吴小青'),
('账号绑定',      8, '下载开户节点', '账号绑定后回传。',                                 '数仓业务表识别判断', '吴小青'),
('AI服务触发',    9, 'AI投顾节点',   'AI服务触发后回传。',                               '数仓业务表识别判断', '吴小青');

-- ----------------------------
-- 投放参数表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `placement_param` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `param_name`    VARCHAR(128) NOT NULL COMMENT '参数名称',
  `param_key`     VARCHAR(64)  NOT NULL COMMENT '参数Key',
  `param_value`   VARCHAR(512) DEFAULT NULL COMMENT '参数Value',
  `channel_code`  VARCHAR(32)  DEFAULT NULL COMMENT '渠道代码: honor/kuaishou/douyin/wechat',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=启用 0=禁用',
  `remark`        VARCHAR(256) DEFAULT NULL COMMENT '备注',
  `creator_id`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `creator_name`  VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投放参数表';

-- ----------------------------
-- 投放链接表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `placement_link` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `link_name`     VARCHAR(128) NOT NULL COMMENT '链接名称',
  `channel_code`  VARCHAR(32)  NOT NULL COMMENT '渠道代码',
  `link_url`      VARCHAR(1024) NOT NULL COMMENT '投放链接URL',
  `deeplink_url`  VARCHAR(1024) DEFAULT NULL COMMENT 'DeepLink URL',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
  `remark`        VARCHAR(256) DEFAULT NULL COMMENT '备注',
  `creator_id`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `creator_name`  VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投放链接表';

-- ----------------------------
-- DeepLink管理表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `deeplink` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `deeplink_name` VARCHAR(128) NOT NULL COMMENT 'DeepLink名称',
  `channel_code`  VARCHAR(32)  NOT NULL COMMENT '渠道代码',
  `deeplink_url`  VARCHAR(1024) NOT NULL COMMENT 'DeepLink URL',
  `target_page`   VARCHAR(256) DEFAULT NULL COMMENT '目标页面',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
  `creator_name`  VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DeepLink管理表';

-- ----------------------------
-- 回传策略表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `conv_strategy` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_name` VARCHAR(128) NOT NULL COMMENT '策略名称',
  `channel`       VARCHAR(100) DEFAULT NULL COMMENT '投放渠道',
  `channel_code`  VARCHAR(32)  NOT NULL COMMENT '渠道代码',
  `description`   VARCHAR(500) DEFAULT NULL COMMENT '策略描述',
  `audit_status`  TINYINT      DEFAULT 0 COMMENT '0=待审核 1=已通过 2=已驳回',
  `audit_remark`  VARCHAR(500) DEFAULT NULL COMMENT '审核备注（驳回原因）',
  `biz_no`        VARCHAR(64)  DEFAULT NULL COMMENT '业务编号',
  `strategy_rule` VARCHAR(512) DEFAULT NULL COMMENT '策略规则',
  `callback_ratio` INT         DEFAULT NULL COMMENT '回传比例（0-100）',
  `ad_info`       VARCHAR(256) DEFAULT NULL COMMENT '广告商',
  `strategy_params` VARCHAR(512) DEFAULT NULL COMMENT '策略参数字符串',
  `node_overview` VARCHAR(512) DEFAULT NULL COMMENT '策略节点概览',
  `monitor_url`   VARCHAR(512) DEFAULT NULL COMMENT '监测链接',
  `node_ids`      VARCHAR(512) DEFAULT NULL COMMENT '关联节点ID列表（逗号分隔）',
  `trigger_rule`  VARCHAR(512) DEFAULT NULL COMMENT '触发规则JSON',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
  `creator_name`  VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回传策略表';

-- ----------------------------
-- 公域数据表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `public_domain_data` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `data_date`     DATE         NOT NULL COMMENT '数据日期',
  `channel_code`  VARCHAR(32)  NOT NULL COMMENT '渠道代码',
  `impression`    BIGINT       DEFAULT 0 COMMENT '曝光量',
  `click`         BIGINT       DEFAULT 0 COMMENT '点击量',
  `download`      BIGINT       DEFAULT 0 COMMENT '下载量',
  `register`      BIGINT       DEFAULT 0 COMMENT '注册量',
  `open_account`  BIGINT       DEFAULT 0 COMMENT '开户量',
  `cost`          DECIMAL(12,2) DEFAULT 0 COMMENT '花费（元）',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_date_channel` (`data_date`, `channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公域数据表';

-- ----------------------------
-- 参数字典表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `param_dict` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `placement_param_id`  BIGINT       NOT NULL COMMENT '关联投放参数ID',
  `product_name`        VARCHAR(128) NOT NULL COMMENT '产品名称',
  `tab_key`             VARCHAR(128) NOT NULL COMMENT '对应tabKey',
  `deleted`             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_placement_param_id` (`placement_param_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数字典表';

-- 初始化参数字典示例数据（渠道编码对应的字典）
INSERT INTO `param_dict` (`placement_param_id`, `product_name`, `tab_key`) VALUES
(1, '国金太阳', 'guojin_sun'),
(1, '国金佣金宝', 'guojin_yjb'),
(1, '国金期货', 'guojin_futures'),
(1, '国金基金', 'guojin_fund');

-- ----------------------------
-- 投放链接表（母链库）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `delivery_link` (
  `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `link_name`            VARCHAR(128) NOT NULL COMMENT '链接名称',
  `channel`              VARCHAR(64)  NOT NULL COMMENT '投放渠道',
  `channel_code`         VARCHAR(64)  DEFAULT NULL COMMENT '渠道代码, 如 douyin/jingdong/kuaishou',
  `content`              VARCHAR(256) DEFAULT NULL COMMENT '投放内容',
  `promo_mode`           VARCHAR(64)  DEFAULT NULL COMMENT '推广模式',
  `origin_url`           VARCHAR(1024) NOT NULL COMMENT '原始投放链接',
  `full_url`             VARCHAR(2048) DEFAULT NULL COMMENT '拼接后完整渠道地址',
  `short_url`            VARCHAR(256)  DEFAULT NULL COMMENT '短链地址',
  `short_url_expire_at`  DATETIME     DEFAULT NULL COMMENT '短链失效时间',
  `link_status`          VARCHAR(32)  DEFAULT 'active' COMMENT '短链状态',
  `params_json`          TEXT         DEFAULT NULL COMMENT '投放参数JSON',
  `creator_name`         VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
  `deleted`              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_channel` (`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投放链接表（母链库）';

-- 初始化母链测试数据
INSERT INTO `delivery_link` (`link_name`, `channel`, `content`, `promo_mode`, `origin_url`, `creator_name`) VALUES
('京东金融-开户引导', '京东金融', '开户引导页', '信息流', 'https://track.jd.com/dl?app=finance&page=open_account', '吴小青'),
('同花顺-模拟炒股', '同花顺', '模拟炒股大赛', '横幅', 'https://t.10jqka.com.cn/flow?act=paper_trading', '吴小青'),
('抖音-极速版下载', '抖音', '极速版APP下载', '短视频', 'https://ad.douyin.com/openapi/sdk/track?app=quick', '吴小青');

-- 初始化一些已审核通过的投放参数
INSERT INTO `placement_param` (`param_name`, `param_key`, `param_value`, `channel_code`, `status`, `category`, `param_usage`, `audit_status`, `auditor`, `creator_name`) VALUES
('渠道来源', 'utm_source', 'jd_finance', '京东金融', 1, '场景参数', '标识投放渠道来源', 1, '管理员', '吴小青'),
('推广活动ID', 'utm_campaign', '', '京东金融', 1, '活动参数', '活动标识', 1, '管理员', '吴小青'),
('推广内容ID', 'utm_content', '', NULL, 1, '场景参数', '区分不同投放内容', 1, '管理员', '吴小青'),
('渠道来源', 'utm_source', '10jqka', '同花顺', 1, '场景参数', '标识投放渠道来源', 1, '管理员', '吴小青'),
('设备ID', 'device_id', '', NULL, 1, '人员参数', '设备唯一标识', 1, '管理员', '吴小青');

-- ----------------------------
-- conv_strategy 表补全字段
-- 说明：若 conv_strategy 表已存在但缺少以下字段，逐条执行。
-- 遇到 "Duplicate column" 错误说明字段已存在，可忽略。
-- ----------------------------
ALTER TABLE `conv_strategy`
  ADD COLUMN `channel` VARCHAR(100) COMMENT '投放渠道' AFTER `strategy_name`;
ALTER TABLE `conv_strategy`
  ADD COLUMN `description` VARCHAR(500) COMMENT '策略描述' AFTER `channel`;
ALTER TABLE `conv_strategy`
  ADD COLUMN `audit_status` TINYINT DEFAULT 0 COMMENT '0=待审核 1=已通过 2=已驳回' AFTER `description`;
ALTER TABLE `conv_strategy`
  ADD COLUMN `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注（驳回原因）' AFTER `audit_status`;

-- ----------------------------
-- 回传策略节点关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `conv_strategy_node` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` BIGINT   NOT NULL COMMENT '策略ID',
  `node_id`     BIGINT   NOT NULL COMMENT '回传节点ID',
  `sort_order`  INT      DEFAULT 0 COMMENT '节点顺序',
  `ad_param`    VARCHAR(256) DEFAULT NULL COMMENT '广告商匹配参数',
  `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回传策略节点关联表';

-- ----------------------------
-- 追加回传节点测试数据（若表为空）
-- ----------------------------
INSERT INTO `conv_node` (`node_name`, `conv_type`, `event_category`, `event_desc`, `event_judge`, `creator_name`)
SELECT * FROM (
  SELECT '下载完成', 10, '行为事件', '用户完成APP下载', '客户端finder', '吴小青'
  UNION ALL SELECT '激活', 11, '行为事件', '用户首次启动APP', '客户端finder', '吴小青'
  UNION ALL SELECT '注册', 12, '行为事件', '用户完成注册登录', 'H5（防方）finder', '吴小青'
  UNION ALL SELECT '付费成功', 13, '交易事件', '用户完成首次付费', '数仓业务表识别判断', '吴小青'
  UNION ALL SELECT '开户', 14, '交易事件', '用户完成证券账户开户', '数仓业务表识别判断', '吴小青'
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `conv_node` WHERE `conv_type` >= 10 AND `conv_type` <= 14);

-- ----------------------------
-- 追加回传策略测试数据
-- ----------------------------
INSERT INTO `conv_strategy` (`strategy_name`, `channel`, `channel_code`, `description`, `audit_status`, `callback_ratio`, `node_overview`, `creator_name`)
SELECT * FROM (
  SELECT '抖音下载回传策略', '抖音', 'douyin', '用于统计抖音渠道广告下载转化', 1, 30, '下载完成 → 激活 → 注册', '吴小青'
  UNION ALL SELECT '快手开户回传策略', '快手', 'kuaishou', '用于统计快手渠道开户转化数据', 1, 50, '下载完成 → 激活 → 开户', '吴小青'
  UNION ALL SELECT '京东金融活动回传', '京东金融', 'jd_finance', '京东渠道活动参与回传', 0, 20, '注册 → 付费成功', '吴小青'
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `conv_strategy` WHERE `channel_code` IN ('douyin', 'kuaishou', 'jd_finance'));
