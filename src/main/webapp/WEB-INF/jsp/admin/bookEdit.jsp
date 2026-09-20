<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${book == null ? '新增图书' : '编辑图书'} - 后台管理</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">${book == null ? '新增图书' : '编辑图书'}</h2>

    <div class="card" style="max-width:900px;">
        <div class="form-row">
            <div class="form-item">
                <label>书名 *</label>
                <input type="text" id="bookName" value="${book.bookName}" maxlength="100">
            </div>
            <div class="form-item">
                <label>作者</label>
                <input type="text" id="author" value="${book.author}" maxlength="50">
            </div>
        </div>
        <div class="form-row">
            <div class="form-item">
                <label>出版社</label>
                <input type="text" id="publisher" value="${book.publisher}" maxlength="50">
            </div>
            <div class="form-item">
                <label>ISBN *</label>
                <input type="text" id="isbn" value="${book.isbn}" maxlength="20">
            </div>
        </div>
        <div class="form-row">
            <div class="form-item">
                <label>分类 *</label>
                <select id="categoryId">
                    <option value="">请选择分类</option>
                    <c:forEach var="c" items="${categories}">
                        <option value="${c.id}" ${book != null && book.categoryId == c.id ? 'selected' : ''}>${c.categoryName}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-item">
                <label>定价（元）</label>
                <input type="number" id="price" value="${book.price}" step="0.01" min="0">
            </div>
        </div>
        <div class="form-row">
            <div class="form-item">
                <label>可借库存 *</label>
                <input type="number" id="stock" value="${book.stock == null ? 0 : book.stock}" min="0">
            </div>
            <div class="form-item">
                <label>馆藏总数 *</label>
                <input type="number" id="totalCount" value="${book.totalCount == null ? 0 : book.totalCount}" min="0">
            </div>
            <div class="form-item">
                <label>馆藏位置</label>
                <input type="text" id="location" value="${book.location}" maxlength="50" placeholder="如：A区-01排">
            </div>
        </div>

        <div class="form-item">
            <label>封面图片（jpg/png/gif，≤2MB${book != null && not empty book.coverUrl ? '，已有封面，重新上传将覆盖' : ''}）</label>
            <input type="file" id="coverFile" accept=".jpg,.jpeg,.png,.gif">
            <img id="coverPreview" src="${not empty book.coverUrl ? (fn:startsWith(book.coverUrl, 'http') ? book.coverUrl : pageContext.request.contextPath.concat(book.coverUrl)) : ''}"
                 style="display:${not empty book.coverUrl ? 'block' : 'none'};margin-top:8px;width:110px;border-radius:4px;box-shadow:0 2px 6px rgba(0,0,0,.15);">
        </div>

        <div class="form-item">
            <label>内容简介</label>
            <textarea id="description" rows="4" maxlength="1000">${book.description}</textarea>
        </div>

        <button class="btn btn-primary" onclick="saveBook()">保 存</button>
        <a class="btn btn-outline" href="${pageContext.request.contextPath}/admin/toBooks">返回列表</a>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
<script>
    var ctx = '${pageContext.request.contextPath}';
    var bookId = ${book == null ? 'null' : book.id};
    var existCover = '${not empty book.coverUrl ? book.coverUrl : ""}';

    /** 封面选择即时预览 */
    $('#coverFile').on('change', function () {
        var file = this.files[0];
        if (!file) return;
        var url = URL.createObjectURL(file);
        $('#coverPreview').attr('src', url).show();
    });

    /**
     * 保存图书：若选择了新封面，先上传封面获取路径，再随表单一并提交
     */
    function saveBook() {
        var data = {
            id: bookId,
            bookName: $.trim($('#bookName').val()),
            author: $.trim($('#author').val()),
            publisher: $.trim($('#publisher').val()),
            isbn: $.trim($('#isbn').val()),
            categoryId: parseInt($('#categoryId').val()) || null,
            price: $('#price').val() ? parseFloat($('#price').val()) : null,
            stock: parseInt($('#stock').val()) || 0,
            totalCount: parseInt($('#totalCount').val()) || 0,
            location: $.trim($('#location').val()),
            description: $.trim($('#description').val()),
            coverUrl: existCover
        };
        if (!data.bookName || !data.isbn || !data.categoryId) {
            alert('书名、ISBN、分类为必填项');
            return;
        }
        var file = $('#coverFile')[0].files[0];
        if (file) {
            // 先上传封面
            var fd = new FormData();
            fd.append('file', file);
            $.ajax({
                url: ctx + '/admin/book/uploadCover',
                type: 'POST',
                data: fd,
                processData: false,
                contentType: false,
                success: function (r) {
                    if (r && r.code === 200) {
                        data.coverUrl = r.data;
                        submitBook(data);
                    } else {
                        alert((r && r.msg) || '封面上传失败');
                    }
                },
                error: function () { alert('封面上传失败，请稍后重试'); }
            });
        } else {
            submitBook(data);
        }
    }

    /** 提交图书表单（有id走更新，无id走新增） */
    function submitBook(data) {
        var url = bookId ? ctx + '/admin/book/update' : ctx + '/admin/book/save';
        $.ajax({
            url: url,
            type: 'POST',
            contentType: 'application/json;charset=UTF-8',
            data: JSON.stringify(data),
            success: function (r) {
                if (r && r.code === 200) {
                    alert(r.msg);
                    window.location.href = ctx + '/admin/toBooks';
                } else {
                    alert((r && r.msg) || '保存失败');
                }
            },
            error: function () { alert('保存失败，请稍后重试'); }
        });
    }
</script>
</body>
</html>