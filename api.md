# 校园图书借阅管理系统 — API 联调文档 & 数据库字段说明

> 版本：v1.0  |  基准代码：以 `src/main/java/com/library/controller/*.java` 和 `library.sql` 实际内容为准
>
> 基础路径：`http://localhost:8080/library`（Tomcat 部署 context = 项目名）

---

## 零、全局约定（所有人必须遵守）

### 0.1 统一响应格式（组长提供）

所有 `@RestController` 接口（除支付宝回调外）返回：

```json
{
  "code": 200,       // 200 成功；401 未登录；403 无权限；500 业务异常；其他业务码自定
  "msg":  "操作成功",
  "data": { ... }    // 业务数据，可为 null
}
```

成功：`Result.ok(data)` 或 `Result.ok("上传成功", data)`
失败：`Result.fail("库存不足")` → code=500

### 0.2 Session 登录态（组员 A 提供）

登录成功后写入 Session：
```
key   = "loginUser"      (UserController.SESSION_USER 常量)
value = User 对象         (含 id/username/role 等)
```

拦截器规则（组长已配）：
- `LoginInterceptor` 拦截：`/borrow/**`, `/user/**`, `/fine/**`, `/deposit/**`, `/notify/**`, `/alipay/**`
- `RoleInterceptor` 拦截：`/admin/**`（session 中 user.role 必须 = 0）

### 0.3 分页参数（通用）

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| pageNum | Integer | 1 | 页码 |
| pageSize | Integer | 10 | 每页条数 |

返回结构：
```json
{
  "total": 120,
  "list": [ ... ],
  "pageNum": 1,
  "pageSize": 10
}
```

### 0.4 HTTP 方法与 Content-Type

| 类型 | 方法 | Content-Type |
|---|---|---|
| 查询 | GET | — |
| 创建/操作 | POST | `application/json`（用 `@RequestBody`） |
| 文件上传 | POST | `multipart/form-data`（用 `@RequestParam("file") MultipartFile`） |

### 0.5 角色枚举（组长 Constants）

| role 值 | 含义 | 借阅额度 | 借阅时长 | 续借次数 | 日罚款 |
|---|---|---|---|---|---|
| 0 | 管理员 | — | — | — | — |
| 1 | 学生 | 5 本 | 30 天 | 1 次 | 0.50 元 |
| 2 | 教师 | 10 本 | 60 天 | 2 次 | 0.30 元 |
| 3 | 访客 | 2 本 | 15 天 | 0 次 | 1.00 元 |

> 以上为初始默认值，管理员可通过 `/admin/user/updatePerm` 按用户或按角色覆盖。

---

## 一、数据库 10 张表字段速查

字符集 `utf8mb4`，引擎 `InnoDB`，库名 `library_ssm`。

### 1.1 t_user 用户表

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK | AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | UNIQUE NOT NULL | — | 登录账号 |
| password | VARCHAR(64) | NOT NULL | — | MD5 加密后存储 |
| real_name | VARCHAR(50) | NOT NULL | — | 真实姓名 |
| role | TINYINT | NOT NULL | — | 0管理员 1学生 2教师 3访客 |
| stu_or_job_no | VARCHAR(30) | UNIQUE | NULL | 学号/工号/访客登记号 |
| phone | VARCHAR(20) | | NULL | 手机号 |
| email | VARCHAR(50) | | NULL | 邮箱 |
| max_borrow_count | INT | NOT NULL | 5 | 借阅额度(本) |
| max_borrow_days | INT | NOT NULL | 30 | 借阅时长(天) |
| max_renew_count | INT | NOT NULL | 1 | 可续借次数 |
| fine_per_day | DECIMAL(6,2) | NOT NULL | 0.50 | 超期日罚款(元/天/本) |
| deposit | DECIMAL(10,2) | NOT NULL | 0.00 | 押金余额(元) |
| status | TINYINT | NOT NULL | 0 | 0正常 1停用 |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 注册时间 |

### 1.2 t_category 分类表

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK | AUTO_INCREMENT | 分类ID |
| category_name | VARCHAR(50) | NOT NULL | — | 分类名称 |
| category_code | VARCHAR(20) | UNIQUE NOT NULL | — | 中图法分类编码（如 TP/I/F/H/K） |
| description | VARCHAR(200) | | NULL | 分类描述 |

### 1.3 t_book 图书表

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK | AUTO_INCREMENT | 图书ID |
| book_name | VARCHAR(100) | NOT NULL | — | 书名 |
| author | VARCHAR(50) | | NULL | 作者 |
| publisher | VARCHAR(50) | | NULL | 出版社 |
| isbn | VARCHAR(20) | UNIQUE NOT NULL | — | ISBN |
| category_id | BIGINT | NOT NULL | — | 关联 t_category.id |
| cover_url | VARCHAR(255) | | NULL | 封面图片相对路径 |
| price | DECIMAL(8,2) | | NULL | 定价 |
| stock | INT | NOT NULL | 0 | 当前可借库存 |
| total_count | INT | NOT NULL | 0 | 馆藏总数 |
| borrow_count | INT | NOT NULL | 0 | 累计借阅次数（热门榜指标） |
| location | VARCHAR(50) | | NULL | 馆藏位置 |
| description | TEXT | | NULL | 内容简介 |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 入库时间 |

### 1.4 t_borrow_record 借阅记录表

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK | AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL | — | 借阅用户ID |
| book_id | BIGINT | NOT NULL | — | 图书ID |
| borrow_date | DATETIME | NOT NULL | — | 借出时间 |
| due_date | DATETIME | NOT NULL | — | 应还时间（= borrow_date + user.max_borrow_days） |
| return_date | DATETIME | | NULL | 实际归还时间 |
| status | TINYINT | NOT NULL | 0 | **0借阅中 1已归还 2超期 3超期已缴费归还** |
| renew_count | INT | NOT NULL | 0 | 已续借次数 |
| fine_amount | DECIMAL(8,2) | NOT NULL | 0.00 | 累计罚款金额 |

### 1.5 t_reservation 预订表

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK | AUTO_INCREMENT | 预订ID |
| user_id | BIGINT | NOT NULL | — | 用户ID |
| book_id | BIGINT | NOT NULL | — | 图书ID |
| reserve_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 预订时间 |
| status | TINYINT | NOT NULL | 0 | **0排队中 1已通知 2已完成 3已取消** |
| notify_time | DATETIME | | NULL | 到书通知时间 |

### 1.6 t_fine_record 罚款记录表

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK | AUTO_INCREMENT | 罚款ID |
| borrow_record_id | BIGINT | NOT NULL | — | 关联 t_borrow_record.id |
| user_id | BIGINT | NOT NULL | — | 用户ID |
| amount | DECIMAL(8,2) | NOT NULL | — | 罚款金额 |
| status | TINYINT | NOT NULL | 0 | **0未缴 1已缴** |
| pay_time | DATETIME | | NULL | 缴纳时间 |
| pay_no | VARCHAR(64) | UNIQUE | NULL | 支付宝交易号（幂等关键字段） |
| operator_id | BIGINT | | NULL | 管理员核销时的操作人ID |

### 1.7 t_deposit_record 押金记录表

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK | AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL | — | 用户ID |
| amount | DECIMAL(10,2) | NOT NULL | — | 金额（正数） |
| type | TINYINT | NOT NULL | — | **1缴纳 2扣罚 3退还** |
| remark | VARCHAR(200) | | NULL | 备注 |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 操作时间 |

### 1.8 t_deposit_refund 押金退还申请表

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK | AUTO_INCREMENT | 申请ID |
| user_id | BIGINT | NOT NULL | — | 用户ID |
| amount | DECIMAL(10,2) | NOT NULL | — | 申请退还金额 |
| reason | VARCHAR(200) | | NULL | 申请原因 |
| status | TINYINT | NOT NULL | 0 | **0申请中 1已通过 2已拒绝** |
| audit_user_id | BIGINT | | NULL | 审核管理员ID |
| audit_remark | VARCHAR(200) | | NULL | 审核备注 |
| audit_time | DATETIME | | NULL | 审核时间 |
| pay_no | VARCHAR(64) | | NULL | 沙箱退款支付宝交易号 |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 申请时间 |

### 1.9 t_notification 站内通知表

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK | AUTO_INCREMENT | 通知ID |
| user_id | BIGINT | NOT NULL | — | 接收用户ID |
| type | INT | NOT NULL | — | 1预订到书 2系统公告 3审核结果 99测试 |
| title | VARCHAR(100) | NOT NULL | — | 通知标题 |
| content | TEXT | | NULL | 通知内容 |
| read_flag | TINYINT | NOT NULL | 0 | **0未读 1已读** |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

### 1.10 t_browse_history 浏览历史表

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK | AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL | — | 用户ID |
| book_id | BIGINT | NOT NULL | — | 图书ID |
| browse_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 浏览时间 |

---

## 二、各模块 API 详细文档

### 组员 A — 账户登录 + 用户管理

#### 2.1.1 用户注册
```
POST /user/register
Content-Type: application/json
Body:
{
  "username": "st03",
  "password": "123456",
  "realName": "王五",
  "role": 1,            // 0管理员 1学生 2教师 3访客
  "stuOrJobNo": "2023003",
  "phone": "13800000005",
  "email": "st03@stu.edu.cn"
}
```
返回：
```json
{"code":200,"msg":"注册成功","data":{"id":6,"username":"st03",...}}
```
业务校验：username 唯一、stuOrJobNo 唯一、MD5 加密密码。

#### 2.1.2 用户登录
```
POST /user/login
Body:
{"username":"st01", "password":"123456"}
```
返回：
```json
{"code":200,"msg":"登录成功","data":{"id":2,"username":"st01","realName":"张小明","role":1,"deposit":50.00,...}}
```
**同时：** 服务端 `session.setAttribute("loginUser", data)` —— 后续请求自动带上 Cookie。

#### 2.1.3 退出登录
```
GET /user/logout
→ 重定向到 /index
```

#### 2.1.4 修改个人资料
```
POST /user/update
Body:
{"realName":"张小名", "phone":"13900000001", "email":"st01@new.edu.cn"}
```
**注意：** 只能改自己的 realName/phone/email，其他字段被忽略。改完后 session 自动同步刷新。

#### 2.1.5 修改密码
```
POST /user/changePwd
Body:
{"oldPassword":"123456", "newPassword":"654321"}
```

---

#### 管理端用户管理（A 负责的 AdminUserController）

#### 2.1.6 读者分页查询
```
GET /admin/user/list?keyword=张&role=1&pageNum=1&pageSize=10
```
| 参数 | 类型 | 说明 |
|---|---|---|
| keyword | String | 模糊匹配 username / realName / stu_or_job_no |
| role | Integer | 1学生 2教师 3访客（0管理员不展示在此列表） |
| pageNum, pageSize | Integer | 分页 |

#### 2.1.7 调整单用户权限参数
```
POST /admin/user/updatePerm
Body:
{"id":2, "maxBorrowCount":6, "maxBorrowDays":30, "maxRenewCount":2, "finePerDay":0.50}
```

#### 2.1.8 启用/停用账号
```
POST /admin/user/updateStatus
Body: {"id":2, "status":1}    // status: 0正常 1停用
```

#### 2.1.9 按角色批量调权限
```
POST /admin/user/batchUpdatePerm
Body:
{
  "role": 1,
  "maxBorrowCount": 5,
  "maxBorrowDays": 30,
  "maxRenewCount": 1,
  "finePerDay": 0.50
}
```

---

### 组员 B — 图书 + 分类管理

#### 2.2.1 分类列表（公开接口，前端下拉筛选用）
```
GET /category/list
无需登录
```
返回：`{"code":200,"data":[{"id":1,"categoryName":"计算机","categoryCode":"TP","description":"..."}, ...]}`

#### 2.2.2 管理端分类 CRUD

| 操作 | Method + Path | Body |
|---|---|---|
| 列表 | `GET /admin/category/list` | — |
| 新增 | `POST /admin/category/save` | `{"categoryName":"哲学","categoryCode":"B","description":"..."}` |
| 修改 | `POST /admin/category/update` | 同上，带 `id` |
| 删除 | `POST /admin/category/delete` | `{"id":6}` |

#### 2.2.3 管理端图书分页查询
```
GET /admin/book/list?keyword=Java&categoryId=1&publisher=机械&author=Bruce&pageNum=1&pageSize=10
```
| 参数 | 说明 |
|---|---|
| keyword | 模糊匹配 book_name / author / isbn |
| categoryId | 精确分类筛选 |
| publisher, author | 精确筛选 |

#### 2.2.4 新增图书
```
POST /admin/book/save
Body:
{
  "bookName": "设计模式",
  "author": "Erich Gamma",
  "publisher": "机械工业出版社",
  "isbn": "9787111075758",
  "categoryId": 1,
  "coverUrl": "/uploads/xxx.jpg",
  "price": 89.00,
  "stock": 3,
  "totalCount": 3,
  "location": "A区-02排",
  "description": "GoF经典..."
}
```

#### 2.2.5 修改图书
```
POST /admin/book/update
Body: 同上，必带 id 字段
```

#### 2.2.6 删除图书
```
POST /admin/book/delete
Body: {"id":1}
```

#### 2.2.7 封面上传（multipart）
```
POST /admin/book/uploadCover
Content-Type: multipart/form-data
Form-Data: file=<文件>
```
业务校验（后端 bookService.uploadCover 里做）：
- 后缀白名单：jpg / png / gif / webp
- 大小 ≤ 2MB
- 存储路径：`webapp/uploads/` 目录
- 文件命名：`UUID.randomUUID() + 原始后缀`

返回：
```json
{"code":200,"msg":"上传成功","data":"/uploads/a1b2c3d4.jpg"}
```
前端拿到 data 后填入表单 coverUrl 字段，再一起调 save/update。

---

### 组长 — 借阅核心（组员 E 消费这些 Service）

#### 组长直接暴露的 Service 方法（组员 E 的 BorrowController 调这些）

```java
Result BorrowService.borrow(User user, Long bookId)           // 线上借阅
Result BorrowService.returnBook(User user, Long recordId)      // 归还（超期自动计费）
Result BorrowService.renew(User user, Long recordId)           // 续借
Result BorrowService.reserve(User user, Long bookId)           // 预订（库存为0时）
Result BorrowService.cancelReserve(User user, Long reserveId)  // 取消预订
```

**BorrowServiceImpl.borrow() 内部事务逻辑（组长实现）：**
```
@Transactional
1. 校验 user.deposit >= 0           押金不为负
2. 校验 当前借阅中数 < user.max_borrow_count
3. UPDATE t_book SET stock = stock-1 WHERE id=? AND stock > 0
   ↓ affected rows = 0 ? throw BusinessException("库存不足") : 继续
4. INSERT t_borrow_record（borrow_date=NOW, due_date=NOW+user.max_borrow_days）
5. UPDATE t_book SET borrow_count = borrow_count+1 WHERE id=?
6. cacheService.evictHot()                 失效 Redis 热门榜
7. cacheService.evictRecommend(user.getId()) 失效用户推荐缓存
8. qqMailService.sendDueReminder(user, book, dueDate)  异步邮件
```

---

### 组员 E — 借阅前端消费 + 页面路由

#### 2.5.1 读者端借阅/续借/预订（E 的 BorrowController）

#### 线上借阅
```
POST /borrow/apply
Body: {"bookId":1}
拦截: LoginInterceptor
```
内部调：`borrowService.borrow(user, bookId)`

#### 归还
```
POST /borrow/return
Body: {"recordId":4}
```
内部调：`borrowService.returnBook(user, recordId)`

#### 续借
```
POST /borrow/renew
Body: {"recordId":4}
```
内部调：`borrowService.renew(user, recordId)`
业务校验：status=0（借阅中）、未超期、renew_count < user.max_renew_count
成功后：due_date = due_date + user.max_borrow_days, renew_count++

#### 预订
```
POST /borrow/reserve/apply
Body: {"bookId":5}
```
业务前置校验：book.stock == 0 才允许预订

#### 取消预订
```
POST /borrow/reserve/cancel
Body: {"reserveId":1}
```

---

#### 2.5.2 管理端借阅记录（E 的 AdminBorrowController）

#### 借阅记录分页查询
```
GET /admin/borrow/list?keyword=Java&status=0&startDate=2025-10-01&endDate=2025-10-08&pageNum=1&pageSize=10
```
| 参数 | 说明 |
|---|---|
| keyword | 模糊匹配 book_name / username / real_name |
| status | 0借阅中 1已归还 2超期 3超期已缴费 |
| startDate, endDate | 借出时间区间筛选（yyyy-MM-dd） |

#### 手动触发超期检查
```
POST /admin/borrow/markOverdue
```
内部调：`borrowService.markOverdue()`（正式由 BorrowTask 定时任务执行）

---

#### 2.5.3 PageController 页面路由（所有 JSP 入口）

> PageController 是 `@Controller`，返回 String → SpringMVC 根据 InternalResourceViewResolver 转发到 WEB-INF/jsp/ 下的 JSP。

#### 读者端页面

| GET Path | 转发到 | 数据来源 |
|---|---|---|
| `/` 或 `/index` | index.jsp | **HomeController** 已处理（动态渲染新书/热门/推荐/到期预警） |
| `/book/toBooks` | reader/books.jsp | bookService 分页图书 + 分类下拉 |
| `/book/detail/{id}` | reader/bookDetail.jsp | bookService.detail(id) + 自动写浏览历史 |
| `/borrow/toMyBorrow` | reader/myBorrow.jsp | borrowService.myBorrows + myReservations + fines + 押金流水 |
| `/user/toProfile` | reader/profile.jsp | user 资料 + 押金信息 |
| `/user/toNotifications` | reader/notifications.jsp | NotificationController 轮询 |

#### 管理端页面

| GET Path | 转发到 |
|---|---|
| `/admin/toMain` | admin/main.jsp |
| `/admin/toBooks` | admin/books.jsp |
| `/admin/toBookEdit?id=` | admin/bookEdit.jsp（id 为空 = 新增） |
| `/admin/toCategories` | admin/categories.jsp |
| `/admin/toUsers` | admin/users.jsp |
| `/admin/toBorrows` | admin/borrows.jsp |
| `/admin/toFines` | admin/fines.jsp |
| `/admin/toRefunds` | admin/refunds.jsp |

---

### 组员 C — 支付宝支付 + 押金 + 罚款

#### 2.3.1 罚款站内直接缴纳（从押金余额扣款）
```
POST /fine/pay
Body: {"fineId":1}
拦截: LoginInterceptor
```
内部：检查 deposit >= amount → `t_user.deposit -= amount` → `t_fine_record.status=1` → 写 `t_deposit_record(type=2扣罚)` → `pay_no = null`（非支付宝）

#### 2.3.2 押金充值（走支付宝）
```
POST /deposit/pay
Body: {"amount":50.00}
```
内部调 `AccountService.payDeposit()` → 返回支付宝下单参数

#### 2.3.3 我的押金信息
```
GET /deposit/my
```
返回：
```json
{
  "code": 200,
  "data": {
    "balance": 50.00,
    "records": [
      {"id":1,"amount":50.00,"type":1,"remark":"注册后首次缴纳押金","createTime":"..."},
      {"id":2,"amount":5.00,"type":2,"remark":"超期罚款扣罚","createTime":"..."}
    ],
    "refunds": [
      {"id":1,"amount":30.00,"reason":"毕业离校","status":0}
    ]
  }
}
```
type: 1缴纳 2扣罚 3退还；refund status: 0申请中 1已通过 2已拒绝

#### 2.3.4 押金退还申请
```
POST /deposit/refund/apply
Body: {"amount":30.00, "reason":"毕业离校"}
```

---

#### 2.3.5 支付宝沙箱下单（AlipayController）
```
POST /alipay/pay
Body: {"type":"FINE", "fineId":1}          // type=FINE 缴纳罚款
      或 {"type":"DEPOSIT", "amount":50.00} // type=DEPOSIT 押金充值
```
订单号生成规则（AlipayController 内硬编码）：
```
FINE_{userId}_{fineId}         // 罚款缴纳
DEP_{userId}                   // 押金充值
```

返回（Mock 模式或真实沙箱，两种情况 data 结构相同）：
```json
{
  "code": 200,
  "data": {
    "payHtml": "<form ...>...</form>",
    "outTradeNo": "FINE_2_1",
    "amount": 5.00,
    "mockMode": false
  }
}
```

#### 2.3.6 支付宝同步回调（用户跳回）
```
GET /alipay/return?out_trade_no=FINE_2_1&total_amount=5.00&trade_status=TRADE_SUCCESS&trade_no=2025...
→ 服务端处理 → 302 重定向到 /alipay/result?success=true&msg=支付成功&...
```

#### 2.3.7 支付宝异步回调（核心！幂等！）
```
POST /alipay/notify     ← 支付宝服务器主动推送
```
**必须公网可访问**（本地开发用 ngrok `ngrok http 8080` 暴露，然后把 alipay.properties 的 `notifyUrl` 设为 `https://xxxx.ngrok-free.app/library/alipay/notify`）。

处理流程：
```
1. 验证签：alipayService.verifySign(params) → 失败 return "fail"
2. 验交易状态：TRADE_SUCCESS 或 TRADE_FINISHED 才处理，其他 return "success"（让支付宝不再重试）
3. 幂等判定：
   ├─ outTradeNo.startsWith("FINE_") → 拆分 userId + fineId
   │   → accountService.confirmFinePaid(userId, fineId, outTradeNo, tradeNo)
   │   → 内部先查 fine_record.pay_no 是否已填，已填直接返回成功
   │   → 未填才开事务：UPDATE fine SET status=1, pay_no=?; 写 deposit_record(type=扣罚); 更新 user.deposit
   │
   └─ outTradeNo.startsWith("DEP_") → 拆分 userId
       → accountService.confirmDepositPaid(userId, amount, outTradeNo, tradeNo)
       → 幂等：先查 deposit_record 有没有同 pay_no
4. 返回 "success"（text/plain）让支付宝停止重试
```

#### 2.3.8 管理端 — 罚款对账

#### 罚款分页查询
```
GET /admin/fine/list?status=0&pageNum=1&pageSize=10    // status: 0未缴 1已缴
```

#### 人工核销单条
```
POST /admin/fine/markPaid
Body: {"id":1}
```
内部：设置 fine_record.status=1, pay_no=null, operator_id=当前管理员, pay_time=NOW

#### 批量核销
```
POST /admin/fine/batchMarkPaid
Body: {"ids":[1,2,3]}
```

---

#### 2.3.9 管理端 — 押金退还审核

#### 退还申请列表
```
GET /admin/refund/list?status=0&pageNum=1&pageSize=10    // 0申请中 1已通过 2已拒绝
```

#### 处理退还申请
```
POST /admin/refund/handle
Body: {"id":1, "status":1, "remark":"批准退还，原路退回到沙箱账户"}
```
status=1（通过）时：
```
1. adminHandle 更新 deposit_refund 表
2. 调 AlipayService.refund(outTradeNo, amount) → 沙箱退款
3. 退款成功后：user.deposit -= amount; 写 t_deposit_record(type=3退还)
```

---

### 组员 D — 首页 + 推荐 + Redis + 通知 + 邮件

#### 2.4.1 分类公开查询
```
GET /category/list
```
（已在 B 的分类里提过，D 也可以直接调用 CategoryService.listAll()）

#### 2.4.2 个性化推荐接口（独立）
```
GET /reco/list
拦截: 不需要（未登录自动降级热门榜）
```
内部：`recommendService.recommend(sessionUser)` → 登录用户走偏好推荐，未登录返回热门榜。

#### 2.4.3 通知中心

#### 通知列表 + 未读数量
```
GET /notify/list
拦截: LoginInterceptor
```
返回：
```json
{
  "code": 200,
  "data": {
    "list": [
      {"id":1,"type":1,"title":"您预订的《百年孤独》已到书","content":"...","readFlag":0,"createTime":"..."},
      ...
    ],
    "unread": 3
  }
}
```

#### 标记单条已读
```
POST /notify/markRead
Body: {"id":1}
```

#### 全部标记已读
```
POST /notify/markAllRead
```

#### 2.4.4 管理端统计总览
```
GET /admin/stats/overview
拦截: RoleInterceptor
```
返回示例：
```json
{
  "code": 200,
  "data": {
    "totalBooks": 12,
    "totalCopies": 55,
    "totalUsers": 5,
    "borrowingCount": 2,
    "overdueCount": 1,
    "unpaidFine": 5.00,
    "categoryDistribution": [
      {"categoryName":"计算机","count":3},
      {"categoryName":"文学","count":3},
      ...
    ],
    "hotTop10": [
      {"id":4,"bookName":"活着","borrowCount":150},
      ...
    ]
  }
}
```

#### 2.4.5 Redis Key 约定（CacheService 内部）

| Key | 类型 | TTL | 用途 | 失效时机 |
|---|---|---|---|---|
| `book:hot` | ZSet（score=borrow_count） | 1800s | 热门借阅榜 TOP10 | 借阅发生时 evictHot() |
| `user:recommend:{userId}` | List | 1800s | 个性化推荐结果 | 借阅/浏览时 evictRecommend(userId) |
| `home:public` | Hash | 900s | 首页公共数据（新书） | — |
| `perm:user:{userId}` | String(JSON) | 按需 | 用户权限参数缓存 | — |

#### 2.4.6 推荐算法流程（RecommendService）

```
recommendService.recommend(user):
  if user == null:
    return bookService.hotTop10()           // 未登录降级热门榜

  if 缓存 hit:
    return 缓存

  // 1. 计算偏好分类权重
  borrowHistory  → 遍历 → category_id 计数 × 权重3
  browseHistory  → 遍历 → category_id 计数 × 权重1
  ↓
  加权求和 → 按权重降序 → 取 TOP3 偏好分类

  // 2. 查询候选集
  在偏好分类下查 stock > 0 的图书
  → 排除 user 当前已借的图书（从 borrow_record 查 status=0 的 book_id）

  // 3. 缓存 + 返回
  结果写入 Redis，TTL=1800s
  return 结果
```

#### 2.4.7 QqMailService 异步邮件（D 提供，组长调）

组长在 BorrowServiceImpl 事务提交后调（避免邮件失败影响主事务）：
```java
qqMailService.sendBorrowReminder(user, book, dueDate);   // 借阅当天发
qqMailService.sendDueWarning(user, books);               // 到期前3天 BorrowTask 发
qqMailService.sendOverdueNotice(user, books);            // 超期时 BorrowTask 发
qqMailService.sendReservationReady(user, book);          // 到书通知时发
```

邮件发送方式：**不要在主事务内同步调用**，用 `TransactionSynchronizationManager.registerSynchronization` 在事务 commit 后触发。

---

## 三、跨模块协作方法调用清单（谁提供 / 谁消费）

| 提供方 | 方法签名 | 消费方 | 场景 |
|---|---|---|---|
| 组长 BorrowService | `Result borrow(User, Long bookId)` | E.BorrowController | 读者借阅 |
| 组长 BorrowService | `Result returnBook(User, Long recordId)` | E.BorrowController | 读者归还 |
| 组长 BorrowService | `Result renew(User, Long recordId)` | E.BorrowController | 读者续借 |
| 组长 BorrowService | `Result reserve(User, Long bookId)` | E.BorrowController | 读者预订 |
| 组长 BorrowService | `Result cancelReserve(User, Long reserveId)` | E.BorrowController | 取消预订 |
| 组长 BorrowService | `int markOverdue()` | BorrowTask（定时） + AdminBorrowController | 超期标记 |
| 组长 BorrowService | `List<BorrowRecord> dueSoon(Long userId)` | D.HomeController | 首页到期预警 |
| 组长 BorrowService | `Set<Long> myBorrowingBookIds(Long userId)` | E.PageController（reader/books.jsp 用） | 已借标记 |
| 组长 PermissionService | `User getUserCached(Long userId)` | A.AccountService, D.HomeController, E.PageController | 获取带权限参数的用户 |
| A.UserService | `Result pageReaders(...)` | AdminUserController | 管理员查读者 |
| A.AccountService | `Result confirmFinePaid(...)` | C.AlipayController | 支付宝回调确认罚款已缴（**幂等**） |
| A.AccountService | `Result confirmDepositPaid(...)` | C.AlipayController | 支付宝回调确认押金已充（**幂等**） |
| A.AccountService | `Result payFine(User, Long fineId)` | AccountController / AlipayController | 押金余额直接扣罚 |
| B.BookService | `Result page(...)` | AdminBookController | 管理端分页 |
| B.BookService | `List<Book> hotTop10()` | D.HomeController + RecommendController | 热门榜 |
| B.BookService | `Book detail(Long id)` | PageController（bookDetail.jsp 用） | 图书详情 |
| C.DepositRefundService | `Result apply(...)` | AccountController | 读者申请退还 |
| C.DepositRefundService | `Result adminHandle(...)` | AdminDepositRefundController | 管理员审核退还 |
| C.DepositRefundService | `List<DepositRefund> myRefunds(Long userId)` | AccountController / profile.jsp | 我的退还申请列表 |
| D.NotificationService | `void save(Long userId, int type, String title, String content)` | 组长 BorrowService（预订到书时）、C（支付成功时） | 写站内通知 |
| D.NotificationService | `void send(Long userId, int type, String title, String content)` | TestController（测试）、BorrowService（预订到书） | 写通知 + 同时尝试发邮件 |
| D.CacheService | `evictHot()`, `evictRecommend(Long userId)` | 组长 BorrowServiceImpl | 借阅后失效缓存 |
| D.RecommendService | `List<Book> recommend(User user)` | HomeController + RecommendController | 推荐 |
| D.StatsService | `Map overview()` | AdminStatsController + PageController(admin/main.jsp) | 管理看板 |

---

## 四、预置账号 & 测试路径速查

| 角色 | 用户名 | 密码 | 测试重点 |
|---|---|---|---|
| 管理员 | `admin` | `admin123` | 后台全部功能 |
| 学生（押金充足） | `st01` | `123456` | 借阅/续借/超期/支付闭环 |
| 学生（押金0） | `st02` | `123456` | 押金充值 → 借阅（《百年孤独》库存为 0，走预订） |
| 教师 | `te01` | `123456` | 借阅额度 10 本 / 60 天（最长） |
| 访客 | `vi01` | `123456` | 借阅额度 2 本 / 15 天 / 日罚款最高 1.00 元 |

**建议联调顺序：**
```
1. st01 登录 → 浏览首页 → 借阅《算法导论》(bookId=3) → 借阅成功
2. 查 home index.jsp → 看到到期预警横幅 + 个性化推荐
3. 再借《人类简史》(bookId=11) → 借阅成功
4. st01 续借《算法导论》→ 续借成功，应还日期 +30 天
5. st02 登录 → 浏览《百年孤独》(bookId=5, stock=0) → 点"预订" → 预订成功
6. 管理员登录 → AdminBorrowController /admin/borrow/markOverdue → 手动触发超期
7. st01 退出再登录 → 首页黄色横幅提示超期
8. st01 /fine/pay 用押金余额缴罚款 → 成功
9. st02 /deposit/pay 充值押金（走支付宝沙箱）→ 充值后借阅
```