<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>分类管理 - 后台管理</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">图书分类管理</h2>

    <%-- ==================== 新增分类 ==================== --%>
    <div class="card">
        <div class="card-title">新增分类</div>
        <div class="inline-form">
            <div class="form-item">
                <label>分类名称</label>
                <input type="text" id="newName" placeholder="如：人工智能" maxlength="50">
            </div>
            <div class="form-item">
                <label>分类编码</label>
                <input type="text" id="newCode" placeholder="如：TP39" maxlength="20">
            </div>
            <div class="form-item">
                <label>描述</label>
                <input type="text" id="newDesc" placeholder="选填" maxlength="200" style="min-width:260px;">
            </div>
            <button class="btn btn-success" onclick="addCategory()">新增</button>
        </div>
    </div>

    <div class="card">
        <table class="table">
            <thead>
            <tr><th>ID</th><th>分类名称</th><th>分类编码</th><th>描述</th><th>图书数量</th><th>操作</th></tr>
            </thead>
            <tbody>
            <c:forEach var="c" items="${categories}">
                <tr>
                    <td>${c.id}</td>
                    <td>${c.categoryName}</td>
                    <td>${c.categoryCode}</td>
                    <td>${c.description}</td>
                    <td>${c.bookCount}</td>
                    <td class="ops">
                        <button class="btn btn-outline btn-sm"
                                onclick="editCategory(${c.id}, '${c.categoryName}', '${c.categoryCode}')">编辑</button>
                        <button class="btn btn-danger btn-sm" onclick="delCategory(${c.id}, '${c.categoryName}')">删除</button>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
<script>
    var ctx = '${pageContext.request.contextPath}';

    /** 新增分类 */
    function addCategory() {
        var name = $.trim($('#newName').val());
        var code = $.trim($('#newCode').val());
        if (!name || !code) {
            alert('分类名称与编码不能为空');
            return;
        }
        ajaxPost(ctx + '/admin/category/save', {
            categoryName: name,
            categoryCode: code,
            description: $.trim($('#newDesc').val())
        }, function (r) {
            alert(r.msg);
            location.reload();
        });
    }

    /** 编辑分类（弹窗输入） */
    function editCategory(id, name, code) {
        var newName = prompt('修改分类名称：', name);
        if (newName === null) return;
        newName = $.trim(newName);
        if (!newName) {
            alert('分类名称不能为空');
            return;
        }
        var newCode = prompt('修改分类编码：', code);
        if (newCode === null) return;
        newCode = $.trim(newCode);
        if (!newCode) {
            alert('分类编码不能为空');
            return;
        }
        var newDesc = prompt('修改描述（可留空）：', '');
        if (newDesc === null) return;
        ajaxPost(ctx + '/admin/category/update', {
            id: id,
            categoryName: newName,
            categoryCode: newCode,
            description: $.trim(newDesc)
        }, function (r) {
            alert(r.msg);
            location.reload();
        });
    }

    /** 删除分类（分类下存在图书时后端拦截） */
    function delCategory(id, name) {
        if (!confirm('确认删除分类「' + name + '」？')) return;
        ajaxPost(ctx + '/admin/category/delete', {id: id}, function (r) {
            alert(r.msg);
            location.reload();
        });
    }
</script>
</body>
</html>
