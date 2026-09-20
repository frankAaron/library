<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>校园图书借阅管理系统 - 首页</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">

    <%-- ==================== 到期预警 / 超期提醒横幅 ==================== --%>
    <c:if test="${not empty sessionScope.loginUser and not empty dueWarn}">
        <c:forEach var="w" items="${dueWarn}">
            <c:choose>
                <c:when test="${w.status == 2}">
                    <div class="warn-banner overdue">
                        【超期提醒】您借阅的《${w.bookName}》已超期未归还，请尽快归还并缴纳罚款，未缴清前无法继续借阅。
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="warn-banner">
                        【到期预警】您借阅的《${w.bookName}》即将到期（应还日期：<fmt:formatDate value="${w.dueDate}" pattern="yyyy-MM-dd"/>），请按时归还或续借。
                    </div>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </c:if>

    <%-- ==================== 搜索区 ==================== --%>
    <div class="hero">
        <h1>校园云图书馆 · 海量好书一键借阅</h1>
        <p>支持按书名 / 作者 / ISBN 关键词查询，还可按分类、出版社精准筛选</p>
        <form class="search-box" action="${pageContext.request.contextPath}/book/toBooks" method="get">
            <input type="text" name="keyword" placeholder="输入书名、作者或ISBN查询图书..." required>
            <button type="submit">立即查询</button>
        </form>
    </div>

    <%-- ==================== 登录用户：借阅概况（动态渲染） ==================== --%>
    <c:if test="${not empty permUser}">
        <div class="dash-grid">
            <div class="dash-card">
                <div class="d-num">${borrowingCount} / ${permUser.maxBorrowCount}</div>
                <div class="d-label">当前在借 / 借阅额度（本）</div>
            </div>
            <div class="dash-card">
                <div class="d-num">${totalBorrow}</div>
                <div class="d-label">累计借阅（次）
                    <c:if test="${activeReader}"><span class="badge badge-orange">阅读达人</span></c:if>
                </div>
            </div>
            <div class="dash-card">
                <div class="d-num">${permUser.maxBorrowDays}天</div>
                <div class="d-label">当前身份借阅时长（超期罚款${permUser.finePerDay}元/天）</div>
            </div>
        </div>
    </c:if>

    <%-- ==================== 分类快捷导航（最顶，独占一行） ==================== --%>
    <div class="card">
        <div class="card-title">分类导航</div>
        <div class="chips">
            <c:forEach var="c" items="${categories}">
                <a class="chip" href="${pageContext.request.contextPath}/book/toBooks?categoryId=${c.id}">
                    ${c.categoryName}<span>${c.bookCount}本</span>
                </a>
            </c:forEach>
        </div>
    </div>

    <%-- ==================== 推荐图书（左） + 热榜（右） 并列 ==================== --%>
    <div style="display:flex; gap:18px; align-items:flex-start;">

        <%-- 推荐区（左，自适应）：登录→个性化推荐；未登录→图书推荐 --%>
        <div style="flex:1;">
            <c:choose>
                <c:when test="${not empty sessionScope.loginUser and not empty recoList}">
                    <div class="card">
                        <div class="card-title">为你推荐 <span class="text-gray" style="font-size:12px;font-weight:normal">基于您的借阅与浏览偏好智能生成</span></div>
                        <div class="book-grid">
                            <c:forEach var="b" items="${recoList}">
                                <a class="book-card" href="${pageContext.request.contextPath}/book/detail/${b.id}">
                                    <div class="cover">
                                        <c:choose>
                                            <c:when test="${not empty b.coverUrl}">
                                                <img src="${fn:startsWith(b.coverUrl, 'http') ? b.coverUrl : pageContext.request.contextPath.concat(b.coverUrl)}" alt="${b.bookName}">
                                            </c:when>
                                            <c:otherwise>
                                                <span class="ph">${fn:substring(b.bookName, 0, 1)}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="info">
                                        <div class="bname" title="${b.bookName}">${b.bookName}</div>
                                        <div class="meta">${b.author}</div>
                                        <div class="foot">
                                            <span class="badge badge-blue">${b.categoryName}</span>
                                            <c:choose>
                                                <c:when test="${b.stock > 0}"><span class="stock-ok">可借 ${b.stock} 本</span></c:when>
                                                <c:otherwise><span class="stock-zero">暂无库存</span></c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </a>
                            </c:forEach>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="card">
                        <div class="card-title">图书推荐</div>
                        <div class="book-grid">
                            <c:forEach var="b" items="${newBooks}">
                                <a class="book-card" href="${pageContext.request.contextPath}/book/detail/${b.id}">
                                    <div class="cover">
                                        <c:choose>
                                            <c:when test="${not empty b.coverUrl}">
                                                <img src="${fn:startsWith(b.coverUrl, 'http') ? b.coverUrl : pageContext.request.contextPath.concat(b.coverUrl)}" alt="${b.bookName}">
                                            </c:when>
                                            <c:otherwise>
                                                <span class="ph">${fn:substring(b.bookName, 0, 1)}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="info">
                                        <div class="bname" title="${b.bookName}">${b.bookName}</div>
                                        <div class="meta">${b.author}</div>
                                        <div class="foot">
                                            <span class="badge badge-blue">${b.categoryName}</span>
                                            <c:choose>
                                                <c:when test="${b.stock > 0}"><span class="stock-ok">可借 ${b.stock} 本</span></c:when>
                                                <c:otherwise><span class="stock-zero">暂无库存</span></c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </a>
                            </c:forEach>
                        </div>
                        <c:if test="${not empty sessionScope.loginUser}">
                            <div class="empty-tip">暂无个性化推荐，借阅或浏览图书后系统将为您智能推荐</div>
                        </c:if>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <%-- 热门借阅榜 TOP10 (右，固定宽度) --%>
        <div class="card" style="flex:0 0 340px;">
            <div class="card-title">热门借阅榜 TOP10</div>
            <ul class="rank-list">
                <c:forEach var="b" items="${hotBooks}" varStatus="st">
                    <li>
                        <span class="rank-no ${st.index < 3 ? 'top' : ''}">${st.index + 1}</span>
                        <a class="rname" href="${pageContext.request.contextPath}/book/detail/${b.id}" title="${b.bookName}">${b.bookName}</a>
                        <span class="text-gray">${b.categoryName}</span>
                        <span class="rcount">${b.borrowCount} 次借阅</span>
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