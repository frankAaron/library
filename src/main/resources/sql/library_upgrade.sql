-- ================================================================
-- 新增表：站内消息表 / 押金退款申请表（增量 DDL）
-- 请在已有 library.sql 基础上单独执行本文件，或合并到初始化脚本
-- ================================================================

use library_ssm;
-- 1. 站内消息表（通知中心）
CREATE TABLE IF NOT EXISTS `t_notification` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    `user_id`    BIGINT       NOT NULL COMMENT '接收用户ID',
    `type`       INT          NOT NULL COMMENT '消息类型 1预订到书 2即将到期 3超期冻结 4罚款生成 5退款结果 99系统',
    `title`      VARCHAR(120) NOT NULL COMMENT '消息标题',
    `content`    VARCHAR(1000) NOT NULL COMMENT '消息内容',
    `is_read`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_read` (`user_id`, `is_read`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息通知';

-- 2. 押金退款申请表
CREATE TABLE IF NOT EXISTS `t_deposit_refund` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    `user_id`      BIGINT         NOT NULL COMMENT '申请人ID',
    `amount`       DECIMAL(10,2)  NOT NULL COMMENT '退款金额（元）',
    `status`       INT            NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已拒绝',
    `reason`       VARCHAR(200)   DEFAULT NULL COMMENT '用户申请原因',
    `admin_remark` VARCHAR(500)   DEFAULT NULL COMMENT '管理员审核备注',
    `apply_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `handle_time`  DATETIME       DEFAULT NULL COMMENT '处理时间',
    `handle_by`    BIGINT         DEFAULT NULL COMMENT '处理人管理员ID',
    INDEX `idx_user` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='押金退款申请';

-- 3. t_deposit_record 的 type 枚举补充（原本已有 3 表示退款，这里仅确认）
-- DEPOSIT_PAY=1 充值  DEPOSIT_DEDUCT=2 扣款  DEPOSIT_REFUND=3 退款
-- 无需 ALTER TABLE，直接在 INSERT 时使用 type=3 即可
