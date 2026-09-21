-- =====================================================================
-- 基于SSM的在线图书借阅管理系统 - 数据库初始化脚本
-- 适用：MySQL 8.0   字符集：utf8mb4   引擎：InnoDB
-- 使用：直接在 MySQL 客户端执行本脚本即可完成建库、建表与演示数据初始化
-- =====================================================================

CREATE DATABASE IF NOT EXISTS library_ssm DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE library_ssm;

-- ---------------------------------------------------------------------
-- 1. 用户表（含三级身份权限参数：学生/教师/访客）
-- role: 0管理员 1学生 2教师 3访客
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
  id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username          VARCHAR(50)   NOT NULL COMMENT '登录账号',
  password          VARCHAR(64)   NOT NULL COMMENT '密码(MD5加密)',
  real_name         VARCHAR(50)   NOT NULL COMMENT '真实姓名',
  role              TINYINT       NOT NULL COMMENT '0管理员 1学生 2教师 3访客',
  stu_or_job_no     VARCHAR(30)   DEFAULT NULL COMMENT '学号/工号/访客登记号',
  phone             VARCHAR(20)   DEFAULT NULL COMMENT '手机号',
  email             VARCHAR(50)   DEFAULT NULL COMMENT '邮箱',
  max_borrow_count  INT           NOT NULL DEFAULT 5  COMMENT '借阅额度(本)',
  max_borrow_days   INT           NOT NULL DEFAULT 30 COMMENT '借阅时长(天)',
  max_renew_count   INT           NOT NULL DEFAULT 1  COMMENT '可续借次数',
  fine_per_day      DECIMAL(6,2)  NOT NULL DEFAULT 0.50 COMMENT '超期日罚款(元/天/本)',
  deposit           DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '押金余额(元)',
  status            TINYINT       NOT NULL DEFAULT 0 COMMENT '0正常 1停用',
  create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_stu_no (stu_or_job_no)
) ENGINE=InnoDB COMMENT='用户表';

-- ---------------------------------------------------------------------
-- 2. 图书分类表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_category;
CREATE TABLE t_category (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  category_name VARCHAR(50)  NOT NULL COMMENT '分类名称',
  category_code VARCHAR(20)  NOT NULL COMMENT '分类编码',
  description   VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (category_code)
) ENGINE=InnoDB COMMENT='图书分类表';

-- ---------------------------------------------------------------------
-- 3. 图书表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_book;
CREATE TABLE t_book (
  id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '图书ID',
  book_name    VARCHAR(100)  NOT NULL COMMENT '书名',
  author       VARCHAR(50)   DEFAULT NULL COMMENT '作者',
  publisher    VARCHAR(50)   DEFAULT NULL COMMENT '出版社',
  isbn         VARCHAR(20)   NOT NULL COMMENT 'ISBN编号',
  category_id  BIGINT        NOT NULL COMMENT '所属分类ID',
  cover_url    VARCHAR(255)  DEFAULT NULL COMMENT '封面图片路径',
  price        DECIMAL(8,2)  DEFAULT NULL COMMENT '定价',
  stock        INT           NOT NULL DEFAULT 0 COMMENT '当前可借库存',
  total_count  INT           NOT NULL DEFAULT 0 COMMENT '馆藏总数',
  borrow_count INT           NOT NULL DEFAULT 0 COMMENT '累计借阅次数(热度指标)',
  location     VARCHAR(50)   DEFAULT NULL COMMENT '馆藏位置',
  description  TEXT          COMMENT '内容简介',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_isbn (isbn),
  KEY idx_name (book_name),
  KEY idx_author (author),
  KEY idx_publisher (publisher),
  KEY idx_category (category_id)
) ENGINE=InnoDB COMMENT='图书表';

-- ---------------------------------------------------------------------
-- 4. 借阅记录表
-- status: 0借阅中 1已归还 2超期(借阅中且已过应还日期) 3超期已缴费归还
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_borrow_record;
CREATE TABLE t_borrow_record (
  id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  user_id      BIGINT        NOT NULL COMMENT '借阅用户ID',
  book_id      BIGINT        NOT NULL COMMENT '图书ID',
  borrow_date  DATETIME      NOT NULL COMMENT '借出时间',
  due_date     DATETIME      NOT NULL COMMENT '应还时间(自动计算)',
  return_date  DATETIME      DEFAULT NULL COMMENT '实际归还时间',
  status       TINYINT       NOT NULL DEFAULT 0 COMMENT '0借阅中 1已归还 2超期 3超期已缴费归还',
  renew_count  INT           NOT NULL DEFAULT 0 COMMENT '已续借次数',
  fine_amount  DECIMAL(8,2)  NOT NULL DEFAULT 0.00 COMMENT '累计罚款金额',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_book (book_id),
  KEY idx_status_due (status, due_date)
) ENGINE=InnoDB COMMENT='借阅记录表';

-- ---------------------------------------------------------------------
-- 5. 图书预订表
-- status: 0排队中 1已通知 2已完成 3已取消
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_reservation;
CREATE TABLE t_reservation (
  id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '预订ID',
  user_id      BIGINT   NOT NULL COMMENT '用户ID',
  book_id      BIGINT   NOT NULL COMMENT '图书ID',
  reserve_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预订时间',
  status       TINYINT  NOT NULL DEFAULT 0 COMMENT '0排队中 1已通知 2已完成 3已取消',
  notify_time  DATETIME DEFAULT NULL COMMENT '到书通知时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_book_status (book_id, status)
) ENGINE=InnoDB COMMENT='图书预订表';

-- ---------------------------------------------------------------------
-- 6. 罚款记录表
-- status: 0未缴 1已缴
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_fine_record;
CREATE TABLE t_fine_record (
  id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '罚款ID',
  borrow_record_id BIGINT        NOT NULL COMMENT '关联借阅记录ID',
  user_id          BIGINT        NOT NULL COMMENT '用户ID',
  amount           DECIMAL(8,2)  NOT NULL COMMENT '罚款金额',
  status           TINYINT       NOT NULL DEFAULT 0 COMMENT '0未缴 1已缴',
  pay_time         DATETIME      DEFAULT NULL COMMENT '缴纳时间',
  operator_id      BIGINT        DEFAULT NULL COMMENT '核销操作管理员ID',
  PRIMARY KEY (id),
  KEY idx_user_status (user_id, status),
  KEY idx_borrow (borrow_record_id)
) ENGINE=InnoDB COMMENT='罚款记录表';

-- ---------------------------------------------------------------------
-- 7. 押金记录表
-- type: 1缴纳 2扣罚 3退还
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_deposit_record;
CREATE TABLE t_deposit_record (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  user_id     BIGINT        NOT NULL COMMENT '用户ID',
  amount      DECIMAL(10,2) NOT NULL COMMENT '金额',
  type        TINYINT       NOT NULL COMMENT '1缴纳 2扣罚 3退还',
  remark      VARCHAR(200)  DEFAULT NULL COMMENT '备注',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id)
) ENGINE=InnoDB COMMENT='押金记录表';

-- ---------------------------------------------------------------------
-- 8. 浏览历史表（个性化推荐数据源）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_browse_history;
CREATE TABLE t_browse_history (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  user_id     BIGINT   NOT NULL COMMENT '用户ID',
  book_id     BIGINT   NOT NULL COMMENT '图书ID',
  browse_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (id),
  KEY idx_user_time (user_id, browse_time)
) ENGINE=InnoDB COMMENT='浏览历史表';

-- =====================================================================
-- 演示数据初始化
-- 默认账号密码均为 123456（管理员为 admin123），密码存储为 MD5 值
--   管理员: admin / admin123
--   学生:   st01(押金50) st02(押金0, 需先充押金才能借阅)
--   教师:   te01 / 123456
--   访客:   vi01 / 123456
-- =====================================================================

-- 1) 用户
INSERT INTO t_user (id, username, password, real_name, role, stu_or_job_no, phone, email,
                    max_borrow_count, max_borrow_days, max_renew_count, fine_per_day, deposit, status) VALUES
(1, 'admin', '0192023a7bbd73250516f069df18b500', '系统管理员', 0, '0001', '13800000000', 'admin@school.edu.cn', 0,  0,  0, 0.00, 0.00,   0),
(2, 'st01',  'e10adc3949ba59abbe56e057f20f883e', '张小明',     1, '2023001', '13800000001', 'st01@stu.edu.cn',    5,  30, 1, 0.50, 50.00,  0),
(3, 'te01',  'e10adc3949ba59abbe56e057f20f883e', '李文华',     2, 'T2008',   '13800000002', 'te01@edu.cn',        10, 60, 2, 0.30, 0.00,   0),
(4, 'vi01',  'e10adc3949ba59abbe56e057f20f883e', '王大伟',     3, 'V2024',   '13800000003', NULL,                 2,  15, 0, 1.00, 100.00, 0),
(5, 'st02',  'e10adc3949ba59abbe56e057f20f883e', '赵晓红',     1, '2023002', '13800000004', 'st02@stu.edu.cn',    5,  30, 1, 0.50, 0.00,   0);

-- 2) 图书分类
INSERT INTO t_category (id, category_name, category_code, description) VALUES
(1, '计算机', 'TP',  '计算机科学与技术类图书'),
(2, '文学',   'I',   '中外文学作品'),
(3, '经济',   'F',   '经济管理类图书'),
(4, '外语',   'H',   '语言学习类图书'),
(5, '历史',   'K',   '历史人文类图书');

-- 3) 图书（百年孤独库存为0，用于演示预订功能）
INSERT INTO t_book (id, book_name, author, publisher, isbn, category_id, cover_url, price,
                    stock, total_count, borrow_count, location, description) VALUES
(1,  'Java编程思想',        'Bruce Eckel',   '机械工业出版社',   '9787111213826', 1, NULL, 108.00, 5,  5, 120, 'A区-01排', 'Java经典入门与进阶必读书籍，全面讲解Java核心思想与设计模式。'),
(2,  '深入理解Java虚拟机',  '周志明',        '机械工业出版社',   '9787111641273', 1, NULL, 129.00, 4,  4, 98,  'A区-01排', 'JVM领域神作，深入讲解类加载、内存模型与垃圾回收机制。'),
(3,  '算法导论',            'Thomas H.Cormen','机械工业出版社',  '9787111407010', 1, NULL, 128.00, 3,  3, 77,  'A区-02排', '算法领域权威著作，覆盖数据结构、图论与动态规划等核心内容。'),
(4,  '活着',                '余华',          '作家出版社',       '9787506365437', 2, NULL, 28.00,  6,  6, 150, 'B区-03排', '余华代表作，讲述福贵一生的苦难与坚韧，感人至深。'),
(5,  '百年孤独',            '加西亚·马尔克斯','南海出版公司',    '9787544253994', 2, NULL, 55.00,  0,  3, 66,  'B区-03排', '魔幻现实主义文学巅峰之作，布恩迪亚家族七代人的传奇故事。'),
(6,  '平凡的世界',          '路遥',          '北京十月文艺出版社','9787530216781', 2, NULL, 108.00, 4, 4, 88,  'B区-04排', '茅盾文学奖获奖作品，全景式描写中国当代城乡社会生活。'),
(7,  '国富论',              '亚当·斯密',     '商务印书馆',       '9787100043196', 3, NULL, 76.00,  3,  3, 45,  'C区-01排', '现代经济学奠基之作，系统阐述国民财富的性质与原因。'),
(8,  '经济学原理',          '曼昆',          '北京大学出版社',   '9787301256519', 3, NULL, 98.00,  5,  5, 30,  'C区-01排', '经典经济学入门教材，微观与宏观经济学双篇合集。'),
(9,  '英语语法大全',        '张道真',        '外语教学与研究出版社','9787513555680', 4, NULL, 49.80, 6, 6, 25, 'D区-01排', '英语语法权威工具书，例句丰富，适合系统学习与查阅。'),
(10, '雅思词汇词根+联想记忆法','俞敏洪',     '群言出版社',       '9787802565650', 4, NULL, 45.00, 5,  5, 60,  'D区-02排', '雅思备考经典词汇书，词根词缀+联想记忆高效背单词。'),
(11, '人类简史',            '尤瓦尔·赫拉利',  '中信出版社',      '9787508647357', 5, NULL, 68.00, 4,  4, 130, 'E区-01排', '从认知革命到人工智能，一部宏大的人类发展史。'),
(12, '明朝那些事儿',        '当年明月',      '浙江人民出版社',   '9787213035654', 5, NULL, 358.00, 3, 3, 40,  'E区-02排', '以史料为基础的明史通俗读物，幽默诙谐且考据严谨。');

-- 4) 借阅记录（覆盖：正常归还 / 借阅中临期 / 超期未还，便于直接演示完整业务闭环）
INSERT INTO t_borrow_record (id, user_id, book_id, borrow_date, due_date, return_date, status, renew_count, fine_amount) VALUES
-- st01 历史归还记录（按时归还）
(1, 2, 1,  DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 40 DAY), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), 1, 0, 0.00),
(2, 2, 2,  DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 35 DAY), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 7  DAY), 1, 0, 0.00),
(3, 2, 4,  DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 DAY), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 1  DAY), 1, 0, 0.00),
-- st01 借阅中且 2 天后到期（演示"到期预警"）
(4, 2, 3,  DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 28 DAY), INTERVAL 30 DAY), NULL, 0, 0, 0.00),
-- st01 超期未还 10 天（演示"超期自动罚款 + 缴费解除限制"）
(5, 2, 12, DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, 2, 0, 5.00),
-- st02 历史记录（计算机类偏好，用于个性化推荐演示）
(6, 5, 1,  DATE_SUB(NOW(), INTERVAL 70 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 70 DAY), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY), 1, 0, 0.00),
(7, 5, 2,  DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 60 DAY), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 32 DAY), 1, 0, 0.00);

-- 5) 罚款记录（st01 超期10天 x 0.5元/天 = 5.00元，未缴纳）
INSERT INTO t_fine_record (id, borrow_record_id, user_id, amount, status) VALUES
(1, 5, 2, 5.00, 0);

-- 6) 预订记录（st02 预订库存为0的《百年孤独》）
INSERT INTO t_reservation (id, user_id, book_id, reserve_time, status) VALUES
(1, 5, 5, DATE_SUB(NOW(), INTERVAL 2 DAY), 0);

-- 7) 押金记录
INSERT INTO t_deposit_record (user_id, amount, type, remark) VALUES
(2, 50.00,  1, '注册后首次缴纳押金'),
(4, 100.00, 1, '注册后首次缴纳押金');

-- 8) 浏览历史（配合推荐算法演示：st01 近期浏览经济类图书，形成多分类偏好）
INSERT INTO t_browse_history (user_id, book_id, browse_time) VALUES
(2, 8,  DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 7,  DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 3,  DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 11, DATE_SUB(NOW(), INTERVAL 8 DAY));
