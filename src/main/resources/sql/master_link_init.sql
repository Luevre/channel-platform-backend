-- 母链库表
CREATE TABLE IF NOT EXISTS master_link (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_name    VARCHAR(100)  NOT NULL COMMENT '产品名称，如 AI选好股',
  base_url        VARCHAR(1000) NOT NULL COMMENT '母链原始URL',
  product_category VARCHAR(50)  COMMENT '产品分类，如 AI投顾产品',
  description     VARCHAR(500)  COMMENT '说明',
  deleted         TINYINT       NOT NULL DEFAULT 0,
  created_at      DATETIME,
  updated_at      DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='母链库';

-- 种子数据：AI选好股
INSERT INTO master_link (product_name, base_url, product_category, description, deleted, created_at, updated_at)
VALUES (
  'AI选好股',
  'https://webapps.yongjinbao.com.cn/comarket/userCenter/bridge-channel.html?menuKey=aiProductIntro&tabKey=AI_SELECT_V2',
  'AI投顾产品',
  'AI选好股智能投顾产品推广落地页',
  0, NOW(), NOW()
);
