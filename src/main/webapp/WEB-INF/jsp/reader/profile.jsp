<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>个人中心 - 校园图书借阅管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">个人中心</h2>

    <div style="display:flex; gap:18px; align-items:flex-start;">
        <%-- ==================== 个人资料修改 ==================== --%>
        <div class="card" style="flex:1;">
            <div class="card-title">个人资料</div>
            <div class="form-item">
                <label>登录账号</label>
                <input type="text" value="${permUser.username}" disabled>
            </div>
            <div class="form-item">
                <label>读者身份</label>
                <input type="text" value="${permUser.role == 1 ? '学生' : (permUser.role == 2 ? '教师' : (permUser.role == 0 ? '管理员' : '访客'))}" disabled>
            </div>
            <div class="form-item">
                <label>真实姓名</label>
                <input type="text" id="realName" value="${permUser.realName}" maxlength="50">
            </div>
            <div class="form-item">
                <label>手机号</label>
                <input type="text" id="phone" value="${permUser.phone}" maxlength="20">
            </div>
            <div class="form-item">
                <label>邮箱</label>
                <input type="text" id="email" value="${permUser.email}" maxlength="50">
            </div>
            <div class="form-item text-gray" style="font-size:12px;">
                学号/工号：${permUser.stuOrJobNo}　注册时间：<fmt:formatDate value="${permUser.createTime}" pattern="yyyy-MM-dd"/>
            </div>
            <button class="btn btn-primary" onclick="saveProfile()">保存资料</button>
        </div>

        <%-- ==================== 修改密码 ==================== --%>
        <div class="card" style="flex:1;">
            <div class="card-title">修改密码</div>
            <div class="form-item">
                <label>原密码</label>
                <input type="password" id="oldPassword" maxlength="32">
            </div>
            <div class="form-item">
                <label>新密码（至少6位）</label>
                <input type="password" id="newPassword" maxlength="32">
            </div>
            <div class="form-item">
                <label>确认新密码</label>
                <input type="password" id="newPassword2" maxlength="32">
            </div>
            <button class="btn btn-primary" onclick="changePwd()">修改密码</button>

            <%-- 押金信息 --%>
            <div class="card-title" style="margin-top:26px;">押金管理</div>
            <div class="form-item">
                <label>当前押金余额（元）</label>
                <input type="text" value="${permUser.deposit}" disabled>
            </div>
            <div class="form-item">
                <label>充值金额（元）</label>
                <input type="number" id="depositAmount" placeholder="单次不超过10000" min="1">
            </div>
            <button class="btn btn-success" onclick="payDeposit()">立即充值</button>
        </div>
    </div>

    <%-- ==================== 押金流水 ==================== --%>
    <div class="card">
        <div class="card-title">押金流水</div>
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
    function saveProfile() {
        var realName = $.trim($('#realName').val());
        if (!realName) {
            alert('姓名不能为空');
            return;
        }
        ajaxPost('${pageContext.request.contextPath}/user/update', {
            realName: realName,
            phone: $.trim($('#phone').val()),
            email: $.trim($('#email').val())
        }, function (r) {
            alert(r.msg);
        });
    }

    function changePwd() {
        var oldPassword = $('#oldPassword').val();
        var newPassword = $('#newPassword').val();
        var newPassword2 = $('#newPassword2').val();
        if (!oldPassword || !newPassword) {
            alert('请填写完整');
            return;
        }
        if (newPassword.length < 6) {
            alert('新密码长度不能少于6位');
            return;
        }
        if (newPassword !== newPassword2) {
            alert('两次输入的新密码不一致');
            return;
        }
        ajaxPost('${pageContext.request.contextPath}/user/changePwd', {
            oldPassword: oldPassword,
            newPassword: newPassword
        }, function (r) {
            alert(r.msg);
            $('#oldPassword,#newPassword,#newPassword2').val('');
        });
    }

    function payDeposit() {
        var amount = $.trim($('#depositAmount').val());
        if (!amount || parseFloat(amount) <= 0) {
            alert('请输入正确的金额');
            return;
        }
        ajaxPost('${pageContext.request.contextPath}/alipay/create', {type:'deposit', amount: amount}, function (r) {
            if (r.data && r.data.payHtml) {
                document.open();
                document.write(r.data.payHtml);
                document.close();
            } else {
                alert('获取支付页面失败');
            }
        });
    }
</script>
</body>
</html>