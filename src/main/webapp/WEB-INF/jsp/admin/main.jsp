<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>后台管理 - 数据看板</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">数据看板</h2>

    <%-- 后台快捷导航 --%>
    <div class="chips" style="margin-bottom:18px;">
        <a class="chip" href="${pageContext.request.contextPath}/admin/toBooks">图书管理</a>
        <a class="chip" href="${pageContext.request.contextPath}/admin/toCategories">分类管理</a>
        <a class="chip" href="${pageContext.request.contextPath}/admin/toUsers">读者管理</a>
        <a class="chip" href="${pageContext.request.contextPath}/admin/toBorrows">借阅记录</a>
        <a class="chip" href="${pageContext.request.contextPath}/admin/toFines">罚款对账</a>
    </div>

    <%-- ==================== 核心统计卡片 ==================== --%>
    <div class="stat-grid">
        <div class="stat-card">
            <div class="num">${stats.bookKinds}</div>
            <div class="label">图书种类</div>
        </div>
        <div class="stat-card">
            <div class="num">${stats.bookTotal}</div>
            <div class="label">馆藏总册数</div>
        </div>
        <div class="stat-card">
            <div class="num">${stats.readerCount}</div>
            <div class="label">注册读者</div>
        </div>
        <div class="stat-card">
            <div class="num">${stats.borrowing}</div>
            <div class="label">在借图书</div>
        </div>
        <div class="stat-card">
            <div class="num danger">${stats.overdue}</div>
            <div class="label">超期未还</div>
        </div>
        <div class="stat-card">
            <div class="num warn">${stats.unpaidFine}</div>
            <div class="label">未缴罚款（元）</div>
        </div>
    </div>

    <div style="display:flex; gap:18px; align-items:flex-start;">
        <%-- ==================== 分类统计 ==================== --%>
        <div class="card" style="flex:1;">
            <div class="card-title">分类馆藏统计</div>
            <table class="table">
                <thead>
                <tr><th>分类名称</th><th>分类编码</th><th>图书数量</th><th>占比</th></tr>
                </thead>
                <tbody>
                <c:forEach var="c" items="${stats.categoryStats}">
                    <tr>
                        <td>${c.categoryName}</td>
                        <td>${c.categoryCode}</td>
                        <td>${c.bookCount}</td>
                        <td>
                            <c:choose>
                                <c:when test="${stats.bookKinds > 0}">
                                    <fmt:formatNumber value="${c.bookCount * 100.0 / stats.bookKinds}" maxFractionDigits="1"/>%
                                </c:when>
                                <c:otherwise>0%</c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>

        <%-- ==================== 热门借阅 TOP10 ==================== --%>
        <div class="card" style="flex:1;">
            <div class="card-title">热门借阅榜 TOP10</div>
            <ul class="rank-list">
                <c:forEach var="b" items="${stats.topBooks}" varStatus="st">
                    <li>
                        <span class="rank-no ${st.index < 3 ? 'top' : ''}">${st.index + 1}</span>
                        <span class="rname" title="${b.bookName}">${b.bookName}</span>
                        <span class="text-gray">${fn:substring(b.categoryName, 0, 6)}</span>
                        <span class="rcount">${b.borrowCount} 次</span>
                    </li>
                </c:forEach>
            </ul>
        </div>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
</body>
</html>
