<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>登录 - 校园图书借阅管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<div class="auth-wrap">
    <div class="auth-card">
        <h2>校园图书借阅管理系统</h2>
        <div class="form-item">
            <label>账号</label>
            <input type="text" id="username" placeholder="请输入登录账号" maxlength="50">
        </div>
        <div class="form-item">
            <label>密码</label>
            <input type="password" id="password" placeholder="请输入密码" maxlength="32"
                   onkeydown="if(event.keyCode===13)doLogin()">
        </div>
        <button class="btn btn-primary" style="width:100%;padding:10px;" onclick="doLogin()">登 录</button>
        <div class="sub">还没有账号？<a href="${pageContext.request.contextPath}/toRegister">立即注册</a>
            &nbsp;|&nbsp;<a href="${pageContext.request.contextPath}/index">返回首页</a>
        </div>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
<script>
    function doLogin() {
        var username = $.trim($('#username').val());
        var password = $.trim($('#password').val());
        if (!username || !password) {
            alert('请输入账号和密码');
            return;
        }
        ajaxPost('${pageContext.request.contextPath}/user/login', {username: username, password: password}, function (r) {
            // 管理员跳转后台首页，读者跳转系统首页
            if (r.data && r.data.role === 0) {
                window.location.href = '${pageContext.request.contextPath}/admin/toMain';
            } else {
                window.location.href = '${pageContext.request.contextPath}/index';
            }
        });
    }
</script>
</body>
</html>
