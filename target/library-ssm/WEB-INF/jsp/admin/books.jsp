<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>图书管理 - 后台管理</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">图书管理</h2>

    <%-- ==================== 查询栏 ==================== --%>
    <div class="card">
        <div class="filter-bar">
            <div class="form-item">
                <label>关键词</label>
                <input type="text" id="qKeyword" placeholder="书名/作者/ISBN">
            </div>
            <div class="form-item">
                <label>分类</label>
                <select id="qCategory">
                    <option value="">全部分类</option>
                    <c:forEach var="c" items="${categories}">
                        <option value="${c.id}">${c.categoryName}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-item">
                <label>出版社</label>
                <input type="text" id="qPublisher">
            </div>
            <div class="form-item">
                <label>作者</label>
                <input type="text" id="qAuthor">
            </div>
            <button class="btn btn-primary" onclick="loadPage(1)">查询</button>
            <a class="btn btn-success" href="${pageContext.request.contextPath}/admin/toBookEdit">新增图书</a>
        </div>
    </div>

    <div class="card">
        <table class="table">
            <thead>
            <tr>
                <th>ID</th><th>封面</th><th>书名</th><th>作者</th><th>出版社</th>
                <th>ISBN</th><th>分类</th><th>库存/总数</th><th>热度</th><th>操作</th>
            </tr>
            </thead>
            <tbody id="bookTbody">
            <tr><td colspan="10"><div class="empty-tip">加载中...</div></td></tr>
            </tbody>
        </table>
        <div class="pagination">
            <button id="prevBtn" onclick="loadPage(pageNum - 1)">上一页</button>
            <span class="page-info" id="pageInfo"></span>
            <button id="nextBtn" onclick="loadPage(pageNum + 1)">下一页</button>
        </div>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
<script>
    var pageNum = 1, totalPages = 1;
    var ctx = '${pageContext.request.contextPath}';

    /** 分页加载图书列表（Ajax） */
    function loadPage(p) {
        pageNum = p;
        var params = {
            keyword: $.trim($('#qKeyword').val()),
            categoryId: $('#qCategory').val(),
            publisher: $.trim($('#qPublisher').val()),
            author: $.trim($('#qAuthor').val()),
            pageNum: pageNum,
            pageSize: 10
        };
        var url = buildUrl(ctx + '/admin/book/list', params);
        $.get(url, function (r) {
            if (!r || r.code !== 200) {
                alert((r && r.msg) || '加载失败');
                return;
            }
            var d = r.data;
            totalPages = d.pages;
            renderRows(d.list);
            $('#pageInfo').text('第 ' + d.pageNum + ' / ' + d.pages + ' 页　共 ' + d.total + ' 条');
            $('#prevBtn').prop('disabled', d.pageNum <= 1);
            $('#nextBtn').prop('disabled', d.pageNum >= d.pages);
        });
    }

    /** 渲染表格行 */
    function renderRows(list) {
        var html = '';
        if (!list || list.length === 0) {
            html = '<tr><td colspan="10"><div class="empty-tip">暂无数据</div></td></tr>';
        }
        $.each(list, function (i, b) {
            var cover = b.coverUrl
                ? '<img class="thumb" src="' + (b.coverUrl.startsWith('http') ? b.coverUrl : ctx + b.coverUrl) + '">'
                : '<span class="thumb">' + b.bookName.charAt(0) + '</span>';
            html += '<tr>'
                + '<td>' + b.id + '</td>'
                + '<td>' + cover + '</td>'
                + '<td>' + b.bookName + '</td>'
                + '<td>' + (b.author || '-') + '</td>'
                + '<td>' + (b.publisher || '-') + '</td>'
                + '<td>' + b.isbn + '</td>'
                + '<td><span class="badge badge-blue">' + (b.categoryName || '-') + '</span></td>'
                + '<td><span class="' + (b.stock > 0 ? 'text-green' : 'text-danger') + '">' + b.stock + '</span> / ' + b.totalCount + '</td>'
                + '<td>' + b.borrowCount + '</td>'
                + '<td class="ops">'
                + '<a class="btn btn-outline btn-sm" href="' + ctx + '/admin/toBookEdit?id=' + b.id + '">编辑</a>'
                + '<button class="btn btn-danger btn-sm" onclick="delBook(' + b.id + ', \'' + b.bookName.replace(/'/g, "\\'") + '\')">删除</button>'
                + '</td></tr>';
        });
        $('#bookTbody').html(html);
    }

    /** 删除图书（存在未归还记录时后端会拦截） */
    function delBook(id, name) {
        if (!confirm('确认删除图书《' + name + '》？删除后不可恢复')) return;
        ajaxPost(ctx + '/admin/book/delete', {id: id}, function (r) {
            alert(r.msg);
            loadPage(pageNum);
        });
    }

    loadPage(1);
</script>
</body>
</html>