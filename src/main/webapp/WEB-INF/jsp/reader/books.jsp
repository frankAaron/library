<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>图书查询 - 校园图书借阅管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">图书查询</h2>

    <div class="card">
        <%-- 多条件筛选表单（GET 提交，支持关键词/分类/出版社/作者组合筛选） --%>
        <form class="filter-bar" action="${pageContext.request.contextPath}/book/toBooks" method="get">
            <div class="form-item">
                <label>关键词</label>
                <input type="text" name="keyword" value="${keyword}" placeholder="书名/作者/ISBN">
            </div>
            <div class="form-item">
                <label>分类</label>
                <select name="categoryId">
                    <option value="">全部分类</option>
                    <c:forEach var="c" items="${categories}">
                        <option value="${c.id}" ${categoryId == c.id ? 'selected' : ''}>${c.categoryName}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-item">
                <label>出版社</label>
                <input type="text" name="publisher" value="${publisher}" placeholder="按出版社筛选">
            </div>
            <div class="form-item">
                <label>作者</label>
                <input type="text" name="author" value="${author}" placeholder="按作者筛选">
            </div>
            <button type="submit" class="btn btn-primary">查询</button>
            <a class="btn btn-outline" href="${pageContext.request.contextPath}/book/toBooks">重置</a>
        </form>
    </div>

    <div class="card">
        <table class="table">
            <thead>
            <tr>
                <th>封面</th>
                <th>书名</th>
                <th>作者</th>
                <th>出版社</th>
                <th>分类</th>
                <th>定价</th>
                <th>库存</th>
                <th>热度</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <c:if test="${empty pageData.list}">
                <tr>
                    <td colspan="9"><div class="empty-tip">未查询到相关图书，请调整筛选条件</div></td>
                </tr>
            </c:if>
            <c:forEach var="b" items="${pageData.list}">
                <tr>
                    <td>
                        <c:choose>
                            <c:when test="${not empty b.coverUrl}">
                                <img class="thumb" src="${fn:startsWith(b.coverUrl, 'http') ? b.coverUrl : pageContext.request.contextPath.concat(b.coverUrl)}" alt="封面">
                            </c:when>
                            <c:otherwise>
                                <span class="thumb">${fn:substring(b.bookName, 0, 1)}</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td><a href="${pageContext.request.contextPath}/book/detail/${b.id}" style="color:#1f4e8c;">${b.bookName}</a></td>
                    <td>${b.author}</td>
                    <td>${b.publisher}</td>
                    <td><span class="badge badge-blue">${b.categoryName}</span></td>
                    <td>${b.price}</td>
                    <td class="${b.stock > 0 ? 'text-green' : 'text-danger'}">${b.stock}</td>
                    <td>${b.borrowCount}</td>
                    <td class="ops">
                        <c:choose>
                            <c:when test="${not empty myBorrowMap and myBorrowMap[b.id]}">
                                <button class="btn btn-disabled btn-sm" disabled>✓ 已借阅</button>
                            </c:when>
                            <c:when test="${not empty myReserveMap and myReserveMap[b.id]}">
                                <button class="btn btn-disabled btn-sm" disabled>✓ 已预订</button>
                            </c:when>
                            <c:when test="${b.stock > 0}">
                                <button class="btn btn-success btn-sm" onclick="doBorrow(${b.id}, this)">借阅</button>
                            </c:when>
                            <c:otherwise>
                                <button class="btn btn-warning btn-sm" onclick="doReserve(${b.id}, this)">预订</button>
                            </c:otherwise>
                        </c:choose>
                        <a class="btn btn-outline btn-sm" href="${pageContext.request.contextPath}/book/detail/${b.id}">详情</a>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

        <%-- 分页（保留筛选参数） --%>
        <div class="pagination">
            <button onclick="goPage(${pageData.pageNum - 1})" ${pageData.pageNum <= 1 ? 'disabled' : ''}>上一页</button>
            <span class="page-info">第 ${pageData.pageNum} / ${pageData.pages} 页　共 ${pageData.total} 条</span>
            <button onclick="goPage(${pageData.pageNum + 1})" ${pageData.pageNum >= pageData.pages ? 'disabled' : ''}>下一页</button>
        </div>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
<script>
    // 分页保留当前筛选条件
    window.__pageUrl = '${pageContext.request.contextPath}/book/toBooks';
    window.__pageParams = {
        keyword: '${keyword}',
        categoryId: '${categoryId}',
        publisher: '${publisher}',
        author: '${author}'
    };

    /** 线上借阅 */
    function doBorrow(bookId, btn) {
        if (btn) btn.disabled = true;
        ajaxPost('${pageContext.request.contextPath}/borrow/apply', {bookId: bookId}, function (r) {
            alert(r.msg);
            location.reload();
        });
        if (btn) setTimeout(function () { btn.disabled = false; }, 3000);
    }

    /** 无库存预订 */
    function doReserve(bookId, btn) {
        if (btn) btn.disabled = true;
        ajaxPost('${pageContext.request.contextPath}/borrow/reserve/apply', {bookId: bookId}, function (r) {
            alert(r.msg);
            location.reload();
        });
        if (btn) setTimeout(function () { btn.disabled = false; }, 3000);
    }
</script>
</body>
</html>