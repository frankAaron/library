# 校园图书借阅管理系统（library-ssm）

基于 **Spring 6 + SpringMVC + MyBatis** 经典 SSM 架构的校园图书借阅管理系统，集成 Redis 缓存、Druid 连接池、定时任务到期预警等能力，支持**四级角色权限**（管理员 / 学生 / 教师 / 访客），覆盖图书管理、在线借阅、预订、超期罚款、押金管理、个性化推荐等完整业务闭环。

---

## 目录

- [技术栈](#技术栈)
- [功能概览](#功能概览)
- [项目结构](#项目结构)
- [数据库设计](#数据库设计)
- [快速开始](#快速开始)
- [核心业务流程](#核心业务流程)
- [创新亮点](#创新亮点)
- [预置账号](#预置账号)

---

## 技术栈

| 分类 | 技术 | 版本 |
|---|---|---|
| JDK | Java | 17 / 21 |
| 前端 | JSP + JSTL + jQuery 3.6 | — |
| 容器 | Tomcat 10（Jakarta Servlet 6） | — |
| 框架 | Spring / SpringMVC | 6.1.14 |
| 持久层 | MyBatis + mybatis-spring | 3.5.13 / 3.0.3 |
| 数据库 | MySQL | 8.0+ |
| 连接池 | Alibaba Druid | 1.2.20 |
| 缓存 | Redis + Jedis | 3.9.0 |
| JSON | Jackson | 2.17.2 |
| 文件上传 | StandardServletMultipartResolver（Spring 6+ 标准 API） | — |
| 日志 | SLF4J + Log4j | 1.7.36 |
| 构建 | Maven | 3.8+ |

---

## 功能概览

### 读者端（学生 / 教师 / 访客）

- 🔍 **图书检索**：关键词模糊匹配（书名 / 作者 / ISBN）+ 分类 / 出版社 / 作者多维筛选 + 分页
- 📖 **图书浏览**：首页图书推荐、热门借阅榜 TOP10、分类快捷导航
- 👤 **借阅概况**：登录后展示当前在借 / 额度、累计借阅次数、身份借阅时长
- 🚨 **到期预警 / 超期提醒**：借阅中 3 天内到期自动横幅预警；超期自动标记并累计罚款
- 📚 **在线借阅**：有库存直接借阅，无库存一键预订到书通知
- 🔁 **续借**：在借未超期图书可续借 1~2 次（按身份配置）
- 💰 **押金充值 / 缴费**：余额不足时提示充值，超期需缴清罚款才能继续借阅
- 🧠 **个性化推荐**：基于借阅历史 + 浏览历史的分类偏好，Redis 缓存推荐候选集
- 📷 **封面预览**：有封面展示图片，无封面展示书名首字占位

### 管理端（管理员）

- 📕 **图书管理**：新增 / 编辑 / 删除图书，封面图片上传（校验后缀 + 大小 + UUID 重命名）
- 🏷️ **分类管理**：图书分类 CRUD
- 📋 **借阅管理**：查看所有借阅记录，手动标记归还
- 👥 **用户管理**：启用 / 停用用户账号，调整借阅额度
- 💸 **罚款核销**：审核并核销读者缴纳的罚款
- 📊 **数据统计**：馆藏总册数、图书总数、借阅活跃度等可视化指标

---

## 项目结构

```
library-ssm/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/library/
    │   ├── common/           # 通用工具
    │   │   ├── Constants.java        # 角色 / 状态码 / Redis Key 常量
    │   │   └── Result.java           # 统一接口响应封装 {code, msg, data}
    │   ├── controller/       # 控制层（REST 风格，@RestController）
    │   │   ├── AccountController.java        # 登录 / 注册
    │   │   ├── HomeController.java           # 首页数据（新书 / 热门 / 推荐）
    │   │   ├── PageController.java           # 页面路由（JSP 转发）
    │   │   ├── BorrowController.java         # 读者端借阅 / 预订 / 续借 / 缴费
    │   │   ├── UserController.java           # 个人中心（借阅概况 / 押金 / 历史）
    │   │   ├── CategoryController.java       # 分类公开接口
    │   │   ├── RecommendController.java      # 个性化推荐接口
    │   │   └── Admin*.java                   # 管理端：图书 / 借阅 / 分类 / 用户 / 罚款 / 统计
    │   ├── dao/              # MyBatis Mapper 接口
    │   │   ├── BookMapper.java
    │   │   ├── BorrowRecordMapper.java
    │   │   ├── CategoryMapper.java
    │   │   ├── UserMapper.java
    │   │   ├── ReservationMapper.java
    │   │   ├── FineRecordMapper.java
    │   │   ├── DepositRecordMapper.java
    │   │   └── BrowseHistoryMapper.java
    │   ├── entity/           # 实体类
    │   │   ├── Book.java           # 图书（含 coverUrl 封面路径）
    │   │   ├── BorrowRecord.java
    │   │   ├── Reservation.java
    │   │   ├── FineRecord.java
    │   │   ├── DepositRecord.java
    │   │   ├── User.java           # 用户（含 role / 借阅额度 / 押金余额）
    │   │   ├── Category.java
    │   │   └── BrowseHistory.java  # 浏览历史（推荐数据源）
    │   ├── service/          # 业务接口
    │   │   ├── BookService.java
    │   │   ├── BorrowService.java
    │   │   ├── CategoryService.java
    │   │   ├── UserService.java
    │   │   ├── AccountService.java
    │   │   ├── CacheService.java           # Redis 缓存封装
    │   │   ├── PermissionService.java      # 角色权限配置
    │   │   ├── RecommendService.java       # 个性化推荐引擎
    │   │   ├── StatsService.java           # 统计指标
    │   │   └── impl/                       # 业务实现（BookServiceImpl 等）
    │   ├── interceptor/      # SpringMVC 拦截器
    │   │   ├── LoginInterceptor.java       # 一级：登录拦截
    │   │   └── RoleInterceptor.java       # 二级：管理员角色拦截（/admin/**）
    │   ├── task/             # Spring Task 定时任务
    │   │   └── BorrowTask.java             # 到期预警扫描 + 超期自动标记
    │   ├── exception/        # 全局异常处理
    │   │   └── GlobalExceptionHandler.java
    │   └── util/             # 工具类
    │       ├── MD5Util.java                # 密码加密
    │       └── DateUtil.java
    ├── resources/
    │   ├── mapper/                    # MyBatis XML
    │   │   ├── BookMapper.xml
    │   │   ├── BorrowRecordMapper.xml
    │   │   ├── CategoryMapper.xml
    │   │   └── ...（其余 Mapper XML）
    │   ├── spring/
    │   │   ├── applicationContext.xml   # Spring 根容器（数据源 / MyBatis / 事务 / Redis）
    │   │   └── springmvc.xml            # SpringMVC 配置（组件扫描 / 拦截器 / 上传 / 静态资源）
    │   ├── sql/
    │   │   └── library.sql             # 建库建表 + 演示数据（完整可执行）
    │   ├── jdbc.properties             # 数据库连接配置
    │   ├── redis.properties            # Redis 连接配置
    │   ├── mybatis-config.xml          # MyBatis 全局配置
    │   └── log4j.properties            # 日志配置
    └── webapp/
        ├── WEB-INF/
        │   ├── jsp/
        │   │   ├── index.jsp                    # 首页（搜索 / 推荐 / 新书 / 热门榜）
        │   │   ├── login.jsp / register.jsp     # 登录 / 注册
        │   │   ├── reader/                      # 读者端页面
        │   │   │   ├── books.jsp                # 图书列表
        │   │   │   ├── bookDetail.jsp           # 图书详情（含封面预览）
        │   │   │   ├── myBorrow.jsp             # 我的借阅
        │   │   │   └── profile.jsp              # 个人中心 / 押金
        │   │   └── admin/                       # 管理端页面
        │   │       ├── main.jsp                 # 管理后台首页
        │   │       ├── books.jsp / bookEdit.jsp # 图书列表 / 编辑
        │   │       ├── borrows.jsp / categories.jsp / users.jsp / fines.jsp
        │   │       └── common/header.jsp
        │   └── web.xml                          # Servlet 容器配置（含 multipart 文件上传限制）
        └── static/
            ├── css/style.css                    # 全局样式
            └── js/common.js                     # 通用 JS（AJAX 封装）
```

---

## 数据库设计

共 **8 张核心表**，字符集 `utf8mb4`，引擎 `InnoDB`。

| 表名 | 说明 | 关键字段 |
|---|---|---|
| `t_user` | 用户表（四级角色） | `role`（0管理员 1学生 2教师 3访客）、借阅额度、押金余额、超期日罚款 |
| `t_category` | 图书分类表 | `category_code`（中图法分类编码） |
| `t_book` | 图书表 | `cover_url` 封面路径、`borrow_count` 累计借阅量（热度指标）、`stock` 可借库存 |
| `t_borrow_record` | 借阅记录表 | `status`（0借阅中 1已归还 2超期 3超期已缴费）、自动计算应还日期 |
| `t_reservation` | 图书预订表 | 排队中 / 已通知 / 已完成 / 已取消 |
| `t_fine_record` | 罚款记录表 | 关联借阅记录、未缴 / 已缴状态 |
| `t_deposit_record` | 押金记录表 | 缴纳 / 扣罚 / 退还三种类型 |
| `t_browse_history` | 浏览历史表 | 个性化推荐数据源 |

完整建表语句和演示数据见 [`src/main/resources/sql/library.sql`](src/main/resources/sql/library.sql)。

---

## 快速开始

### 环境要求

- JDK 21+（pom 中同时配置了 `source=21` 和 maven-compiler `source/target=21`）
- Maven 3.8+
- MySQL 8.0+
- Redis（热门榜 / 权限 / 推荐缓存，可选但推荐）
- Tomcat 10.1+（Jakarta Servlet 6 容器）

### 步骤一：初始化数据库

```bash
mysql -u root -p < src/main/resources/sql/library.sql
```

该脚本会**自动创建数据库 `library_ssm`**、建表并注入演示数据，无需手动先建库。

### 步骤二：修改配置

根据你的环境修改两个 properties 文件：

**`src/main/resources/jdbc.properties`**

```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/library_ssm?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
jdbc.username=root
jdbc.password=你的密码
jdbc.initialSize=5
jdbc.maxActive=20
jdbc.minIdle=3
jdbc.maxWait=60000
```

**`src/main/resources/redis.properties`**

```properties
redis.host=localhost
redis.port=6379
redis.password=      # 无密码则留空
redis.maxTotal=32
redis.maxIdle=16
redis.minIdle=4
redis.timeout=3000
```

> Redis 连接在 `applicationContext.xml` 中配置了 `ignore-unresolvable="true"`，Redis 未启动时仅影响缓存功能，核心业务仍可运行。

### 步骤三：Maven 构建

```bash
cd library-ssm
mvn clean package -DskipTests
```

构建产物为 `target/library-ssm.war`。

### 步骤四：部署到 Tomcat 10

将 `library-ssm.war` 复制到 `$TOMCAT_HOME/webapps/` 下，启动 Tomcat：

```bash
# Windows
$TOMCAT_HOME/bin/startup.bat

# macOS / Linux
$TOMCAT_HOME/bin/startup.sh
```

启动成功后，浏览器访问：

```
http://localhost:8080/library-ssm/
```

即可看到首页。

---

## 核心业务流程

### 📚 借阅流程

```
读者浏览图书 → 点击"立即借阅" 
  ├─ 押金余额不足 → 提示先充值押金
  ├─ 当前借阅数 ≥ 额度 → 提示借阅已达上限
  └─ 条件通过 → 事务内扣库存(deductStock) + 生成借阅记录 + 累计借阅次数+1 + 失效热门榜缓存
```

### 🔁 续借流程

```
借阅中图书 → 点击"续借"
  ├─ 已超期 → 禁止续借，需先缴费归还
  ├─ 已达最大续借次数 → 提示已达上限
  └─ 通过 → 更新应还日期 = 原应还日期 + 借阅时长，renew_count+1
```

### 🪙 超期自动处理

```
定时任务 BorrowTask（Spring Task，每分钟扫描）
  ├─ 借阅中且 now() > due_date → status 改为"超期"
  ├─ 超期天数 × 日罚款 → 累计到 fine_amount
  └─ 到期前 3 天 → 标记到期预警（登录时横幅展示）
```

### 🧠 个性化推荐

```
登录用户 → 首页推荐
  ├─ 浏览历史 + 借阅历史 → 提取偏好分类（权重：借阅 > 浏览）
  ├─ 在偏好分类下查询有库存图书 → 排除已借图书
  └─ 返回推荐列表并缓存到 Redis

未登录 / 无历史 → 降级为图书推荐列表
```

### 📷 封面上传

```
管理后台编辑图书 → 选择封面文件
  ├─ 前端即时预览（URL.createObjectURL）
  ├─ 提交时先调用 /admin/book/uploadCover → 校验后缀 / 大小 → UUID 重命名 → 存入 /uploads/
  └─ 拿到返回路径后，随图书表单 JSON 提交保存 coverUrl 字段
```

---

## 创新亮点

### 1. 三级拦截器权限体系
- **一级 `LoginInterceptor`**：拦截所有页面 + `/admin/**` + `/borrow/**` 等受限路径
- **二级 `RoleInterceptor`**：专门拦截 `/admin/**`，校验 session 中的角色是否为管理员
- 图书浏览对未登录开放，借阅 / 预订动作在业务层二次校验登录

### 2. 热门图书 Redis ZSet 榜单缓存
- 借阅发生时失效榜单缓存（`evictHot()`）
- 首页请求时先查 Redis ZSet，命中则按榜单顺序组装；未命中回源数据库并重建缓存（1800s TTL）
- 榜单 member=图书ID，score=累计借阅量，天然支持按热度排序

### 3. 条件更新防止并发超卖
- `deductStock` 使用 `UPDATE t_book SET stock = stock - 1 WHERE id = ? AND stock > 0`
- 配合 Spring `@Transactional`，库存为 0 时 `UPDATE` 影响行数为 0，Service 层据此判定失败

### 4. 定时任务到期预警 + 超期自动标记
- `BorrowTask` 每分钟扫描一次
- 到期前 3 天标记预警 → 登录时横幅展示
- 超期自动累计罚款 → 未缴清前禁止再次借阅

### 5. 多条件模糊检索
- 关键词同时匹配书名 / 作者 / ISBN
- 分类 / 出版社 / 作者 作为精确筛选条件
- 分页查询 + 总数统计分离，SQL 写在 `<sql>` 片段中复用

### 6. 个性化推荐引擎
- 数据源：借阅历史（高权重）+ 浏览历史（低权重）
- 计算偏好分类 → 在偏好分类下查询有库存图书 → 排除已借
- 结果缓存到 Redis，图书推荐 / 借阅变化时主动失效

### 7. Spring 6+ 文件上传
- 使用 `StandardServletMultipartResolver`（无需额外引入 commons-fileupload）
- 配置在 `web.xml` 中设置全局上传限制（3MB / 10MB）
- 封面上传额外校验后缀 + 大小，UUID 重命名防止路径冲突

### 8. 统一响应封装 + 全局异常处理
- 所有 REST 接口返回 `Result {code: 200/500, msg, data}`
- `GlobalExceptionHandler` 统一捕获业务异常和系统异常，前端按 `code` 判断成功失败

---

## 预置账号

所有密码均为 `123456`（管理员为 `admin123`），存储为 MD5 值。

| 角色 | 用户名 | 密码 | 说明 |
|---|---|---|---|
| 管理员 | `admin` | `admin123` | 管理后台全功能 |
| 学生 | `st01` | `123456` | 押金 50 元（可借阅） |
| 学生 | `st02` | `123456` | 押金 0 元（需先充值） |
| 教师 | `te01` | `123456` | 借阅额度 10 本 / 60 天 |
| 访客 | `vi01` | `123456` | 借阅额度 2 本 / 15 天 |

---


# 1. 查谁占了 1099
netstat -ano | findstr ":1099"

# 2. 杀掉它（把 PID 换成查到的那个数字）
taskkill /F /PID 20132

## 许可证

仅供学习交流使用。