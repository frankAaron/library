<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>我的借阅 - 校园图书借阅管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">我的借阅</h2>

    <%-- ==================== 账户概览 ==================== --%>
    <div class="dash-grid">
        <div class="dash-card">
            <div class="d-num">${permUser == null ? '-' : permUser.maxBorrowCount}</div>
            <div class="d-label">借阅额度（本）　借期 ${permUser == null ? '-' : permUser.maxBorrowDays} 天</div>
        </div>
        <div class="dash-card">
            <div class="d-num">${permUser == null ? '-' : permUser.deposit}</div>
            <div class="d-label">押金余额（罚款从押金中扣除）</div>
        </div>
        <div class="dash-card">
            <div class="d-num">${permUser == null ? '-' : permUser.maxRenewCount}</div>
            <div class="d-label">每本可续借次数（${permUser == null ? '-' : permUser.finePerDay} 元/天超期罚款）</div>
        </div>
    </div>

    <%-- ==================== 我的借阅记录 ==================== --%>
    <div class="card">
        <div class="card-title">借阅记录</div>
        <table class="table">
            <thead>
            <tr>
                <th>书名</th>
                <th>借出时间</th>
                <th>应还时间</th>
                <th>状态</th>
                <th>续借次数</th>
                <th>罚款</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <c:if test="${empty borrows}">
                <tr><td colspan="7"><div class="empty-tip">暂无借阅记录，去<a href="${pageContext.request.contextPath}/book/toBooks" style="color:#1f4e8c;">查询图书</a>开启阅读之旅吧</div></td></tr>
            </c:if>
            <c:forEach var="r" items="${borrows}">
                <tr>
                    <td>${r.bookName}</td>
                    <td><fmt:formatDate value="${r.borrowDate}" pattern="yyyy-MM-dd"/></td>
                    <td><fmt:formatDate value="${r.dueDate}" pattern="yyyy-MM-dd"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${r.status == 0}"><span class="badge badge-blue">借阅中</span></c:when>
                            <c:when test="${r.status == 1}"><span class="badge badge-green">已归还</span></c:when>
                            <c:when test="${r.status == 2}"><span class="badge badge-red">超期未还</span></c:when>
                            <c:otherwise><span class="badge badge-gray">超期已归</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td>${r.renewCount}</td>
                    <td class="${r.fineAmount > 0 ? 'text-danger' : ''}">${r.fineAmount}</td>
                    <td class="ops">
                        <c:if test="${r.status == 0 or r.status == 2}">
                            <button class="btn btn-primary btn-sm" onclick="doReturn(${r.id})">归还</button>
                            <c:if test="${r.status == 0}">
                                <button class="btn btn-warning btn-sm" onclick="doRenew(${r.id})">续借</button>
                            </c:if>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>

    <div style="display:flex; gap:18px; align-items:flex-start;">
        <%-- ==================== 我的预订 ==================== --%>
        <div class="card" style="flex:1;">
            <div class="card-title">我的预订</div>
            <table class="table">
                <thead>
                <tr><th>书名</th><th>预订时间</th><th>状态</th><th>操作</th></tr>
                </thead>
                <tbody>
                <c:if test="${empty reservations}">
                    <tr><td colspan="4"><div class="empty-tip">暂无预订记录</div></td></tr>
                </c:if>
                <c:forEach var="r" items="${reservations}">
                    <tr>
                        <td>${r.bookName}</td>
                        <td><fmt:formatDate value="${r.reserveTime}" pattern="yyyy-MM-dd HH:mm"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${r.status == 0}"><span class="badge badge-blue">排队中</span></c:when>
                                <c:when test="${r.status == 1}"><span class="badge badge-orange">已通知到书</span></c:when>
                                <c:when test="${r.status == 2}"><span class="badge badge-green">已完成</span></c:when>
                                <c:otherwise><span class="badge badge-gray">已取消</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="ops">
                            <c:if test="${r.status == 0}">
                                <button class="btn btn-danger btn-sm" onclick="doCancelReserve(${r.id})">取消</button>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>

        <%-- ==================== 我的罚款 ==================== --%>
        <div class="card" style="flex:1;">
            <div class="card-title">我的罚款</div>
            <table class="table">
                <thead>
                <tr><th>图书</th><th>金额</th><th>状态</th><th>操作</th></tr>
                </thead>
                <tbody>
                <c:if test="${empty fines}">
                    <tr><td colspan="4"><div class="empty-tip">暂无罚款记录，继续保持好习惯</div></td></tr>
                </c:if>
                <c:forEach var="f" items="${fines}">
                    <tr>
                        <td>${f.bookName}</td>
                        <td class="text-danger">${f.amount}</td>
                        <td>
                            <c:choose>
                                <c:when test="${f.status == 0}"><span class="badge badge-red">未缴</span></c:when>
                                <c:otherwise><span class="badge badge-green">已缴</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="ops">
                            <c:if test="${f.status == 0}">
                                <button class="btn btn-primary btn-sm" onclick="doPayFine(${f.id})">缴纳</button>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <%-- ==================== 押金流水 ==================== --%>
    <div class="card">
        <div class="card-title">
            押金流水
            <button class="btn btn-success btn-sm" style="float:right;" onclick="doPayDeposit()">押金充值</button>
        </div>
        <table class="table">
            <thead>
            <tr><th>时间</th><th>类型</th><th>金额（元）</th><th>备注</th></tr>
            </thead>
            <tbody>
            <c:if test="${empty depositRecords}">
                <tr><td colspan="4"><div class="empty-tip">暂无押金流水</div></td></tr>
            </c:if>
            <c:forEach var="d" items="${depositRecords}">
                <tr>
                    <td><fmt:formatDate value="${d.createTime}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${d.type == 1}"><span class="badge badge-green">缴纳</span></c:when>
                            <c:when test="${d.type == 2}"><span class="badge badge-red">罚款扣款</span></c:when>
                            <c:otherwise><span class="badge badge-blue">退款</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td>${d.amount}</td>
                    <td>${d.remark}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
<script>
    function doReturn(recordId) {
        Modal.confirm('归还图书', '确认归还该图书？若已超期将自动计算罚款。', function () {
            ajaxPost('${pageContext.request.contextPath}/borrow/return', {recordId: recordId}, function (r) {
                Toast.ok(r.msg || '归还成功');
                location.reload();
            });
        });
    }

    function doRenew(recordId) {
        ajaxPost('${pageContext.request.contextPath}/borrow/renew', {recordId: recordId}, function (r) {
            Toast.ok(r.msg || '续借成功');
            location.reload();
        });
    }

    function doCancelReserve(reserveId) {
        Modal.confirm('取消预订', '确认取消该预订？', function () {
            ajaxPost('${pageContext.request.contextPath}/borrow/reserve/cancel', {reserveId: reserveId}, function (r) {
                Toast.ok(r.msg || '已取消');
                location.reload();
            });
        });
    }

    function doPayFine(fineId) {
        ajaxPost('${pageContext.request.contextPath}/alipay/create', {type:'fine', fineId: fineId}, function (r) {
            if (r.data && r.data.payHtml) {
                document.open();
                document.write(r.data.payHtml);
                document.close();
            } else {
                Toast.err('获取支付页面失败');
            }
        });
    }

    function doPayDeposit() {
        Modal.prompt('押金充值', '请输入充值金额（元），例如 10', function (amount) {
            amount = $.trim(amount);
            if (!amount || isNaN(amount) || parseFloat(amount) <= 0) {
                Toast.err('请输入正确的金额');
                return;
            }
            ajaxPost('${pageContext.request.contextPath}/deposit/pay', {amount: amount}, function (r) {
                Toast.ok(r.msg || '充值成功');
                location.reload();
            });
        });
    }
</script>
</body>
</html>