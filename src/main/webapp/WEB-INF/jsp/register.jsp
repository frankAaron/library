<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>注册 - 校园图书借阅管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<div class="auth-wrap" style="max-width:520px;">
    <div class="auth-card">
        <h2>读者注册</h2>
        <div class="form-item">
            <label>登录账号 *</label>
            <input type="text" id="username" placeholder="请输入登录账号" maxlength="50">
        </div>
        <div class="form-row">
            <div class="form-item">
                <label>密码（至少6位）*</label>
                <input type="password" id="password" placeholder="请输入密码" maxlength="32">
            </div>
            <div class="form-item">
                <label>确认密码 *</label>
                <input type="password" id="password2" placeholder="请再次输入密码" maxlength="32">
            </div>
        </div>
        <div class="form-row">
            <div class="form-item">
                <label>真实姓名 *</label>
                <input type="text" id="realName" placeholder="请输入真实姓名" maxlength="50">
            </div>
            <div class="form-item">
                <label>读者身份 *</label>
                <select id="role">
                    <option value="1">学生</option>
                    <option value="2">教师</option>
                    <option value="3">访客</option>
                </select>
            </div>
        </div>
        <div class="form-row">
            <div class="form-item">
                <label>学号 / 工号</label>
                <input type="text" disabled value="系统将自动生成" style="color:#999;background:#f5f5f5;">
            </div>
            <div class="form-item">
                <label>手机号</label>
                <input type="text" id="phone" placeholder="选填" maxlength="20">
            </div>
        </div>
        <div class="form-item">
            <label>邮箱</label>
            <input type="text" id="email" placeholder="选填" maxlength="50">
        </div>
        <div class="form-item text-gray" style="font-size:12px;line-height:1.8;">
            注册提示：学生需缴押金50元、访客需缴押金100元后方可借阅；教师免押金。各身份借阅额度、时长与罚款标准不同。
        </div>
        <button class="btn btn-primary" style="width:100%;padding:10px;" onclick="doRegister()">注 册</button>
        <div class="sub">已有账号？<a href="${pageContext.request.contextPath}/toLogin">去登录</a></div>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
<script>
    function doRegister() {
        var username = $.trim($('#username').val());
        var password = $('#password').val();
        var password2 = $('#password2').val();
        var realName = $.trim($('#realName').val());
        if (!username || !password || !realName) {
            alert('账号、密码、姓名均不能为空');
            return;
        }
        if (password.length < 6) {
            alert('密码长度不能少于6位');
            return;
        }
        if (password !== password2) {
            alert('两次输入的密码不一致');
            return;
        }
        ajaxPost('${pageContext.request.contextPath}/user/register', {
            username: username,
            password: password,
            realName: realName,
            role: parseInt($('#role').val()),
            phone: $.trim($('#phone').val()),
            email: $.trim($('#email').val())
        }, function (r) {
            alert(r.msg);
            window.location.href = '${pageContext.request.contextPath}/toLogin';
        });
    }
</script>
</body>
</html>