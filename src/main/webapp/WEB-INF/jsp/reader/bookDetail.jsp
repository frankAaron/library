<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${book.bookName} - 图书详情</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">图书详情</h2>

    <div class="detail-wrap">
        <%-- 封面（无封面显示书名首字占位） --%>
        <div class="detail-cover">
            <c:choose>
                <c:when test="${not empty book.coverUrl}">
                    <img src="${fn:startsWith(book.coverUrl, 'http') ? book.coverUrl : pageContext.request.contextPath.concat(book.coverUrl)}" alt="${book.bookName}">
                </c:when>
                <c:otherwise>
                    <div class="ph-box">${fn:substring(book.bookName, 0, 1)}</div>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="detail-info">
            <h2>${book.bookName}</h2>
            <table class="info-table">
                <tr>
                    <td>作者</td><td>${book.author}</td>
                    <td>分类</td><td><span class="badge badge-blue">${category.categoryName}</span></td>
                </tr>
                <tr>
                    <td>出版社</td><td>${book.publisher}</td>
                    <td>ISBN</td><td>${book.isbn}</td>
                </tr>
                <tr>
                    <td>定价</td><td>${book.price} 元</td>
                    <td>馆藏位置</td><td>${book.location}</td>
                </tr>
                <tr>
                    <td>可借库存</td>
                    <td class="${book.stock > 0 ? 'text-green' : 'text-danger'}">${book.stock} 本</td>
                    <td>馆藏总数</td><td>${book.totalCount} 本</td>
                </tr>
                <tr>
                    <td>累计借阅</td><td>${book.borrowCount} 次</td>
                    <td>入库时间</td><td><fmt:formatDate value="${book.createTime}" pattern="yyyy-MM-dd"/></td>
                </tr>
            </table>

            <c:choose>
                <c:when test="${not empty myBorrow}">
                    <button class="btn btn-disabled" disabled>✓ 已借阅（应还：${fn:substring(myBorrow.dueDate, 0, 10)}）</button>
                </c:when>
                <c:when test="${myReserving}">
                    <button class="btn btn-disabled" disabled>✓ 已预订，请等待到书通知</button>
                </c:when>
                <c:when test="${book.stock > 0}">
                    <button class="btn btn-success" onclick="doBorrow(${book.id})">立即借阅</button>
                </c:when>
                <c:otherwise>
                    <button class="btn btn-warning" onclick="doReserve(${book.id})">预订到书通知</button>
                </c:otherwise>
            </c:choose>
            <a class="btn btn-outline" href="${pageContext.request.contextPath}/book/toBooks">返回列表</a>

            <c:if test="${not empty book.description}">
                <div class="desc"><b>内容简介：</b>${book.description}</div>
            </c:if>
        </div>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
<script>
    /** 借阅 */
    function doBorrow(bookId) {
        ajaxPost('${pageContext.request.contextPath}/borrow/apply', {bookId: bookId}, function (r) {
            alert(r.msg);
            location.reload();
        });
    }

    /** 预订 */
    function doReserve(bookId) {
        ajaxPost('${pageContext.request.contextPath}/borrow/reserve/apply', {bookId: bookId}, function (r) {
            alert(r.msg);
            location.reload();
        });
    }
</script>
</body>
</html>