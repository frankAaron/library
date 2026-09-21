<%--
  全站公共顶部导航（各页面 include 复用）
  依赖：Session 中的 loginUser（User对象）
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<header class="navbar">
    <div class="nav-inner">
        <a class="logo" href="${pageContext.request.contextPath}/index">校园图书借阅管理系统</a>
        <nav class="nav-links">
            <a href="${pageContext.request.contextPath}/index">首页</a>
            <a href="${pageContext.request.contextPath}/book/toBooks">图书查询</a>
            <c:if test="${not empty sessionScope.loginUser}">
                <a href="${pageContext.request.contextPath}/borrow/toMyBorrow">我的借阅</a>
                <a href="${pageContext.request.contextPath}/user/toProfile">个人中心</a>
                <c:if test="${sessionScope.loginUser.role == 0}">
                    <a href="${pageContext.request.contextPath}/admin/toMain">后台管理</a>
                </c:if>
            </c:if>
        </nav>
        <div class="nav-user">
            <c:choose>
                <c:when test="${empty sessionScope.loginUser}">
                    <a class="btn btn-outline btn-sm" href="${pageContext.request.contextPath}/toLogin">登录</a>
                    <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/toRegister">注册</a>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/user/toNotifications" class="notify-bell" title="消息中心">
                        <span class="bell-icon">🔔</span>
                        <span id="unreadDot" class="badge-dot" style="display:none;">0</span>
                    </a>
                    <span class="welcome">
                        ${sessionScope.loginUser.realName}
                        <c:choose>
                            <c:when test="${sessionScope.loginUser.role == 0}">（管理员）</c:when>
                            <c:when test="${sessionScope.loginUser.role == 1}">（学生）</c:when>
                            <c:when test="${sessionScope.loginUser.role == 2}">（教师）</c:when>
                            <c:otherwise>（访客）</c:otherwise>
                        </c:choose>
                    </span>
                    <a class="btn btn-outline btn-sm" href="${pageContext.request.contextPath}/user/logout">退出</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>

<style>
.notify-bell{position:relative;padding:6px 8px;margin-right:6px;font-size:18px;text-decoration:none;}
.notify-bell:hover{opacity:.8;}
.notify-bell .badge-dot{position:absolute;top:-2px;right:-2px;background:#e74c3c;color:#fff;font-size:11px;min-width:16px;height:16px;border-radius:8px;display:flex;align-items:center;justify-content:center;padding:0 4px;line-height:1;font-weight:bold;box-shadow:0 0 0 2px #fff;}
</style>

<c:if test="${not empty sessionScope.loginUser}">
<script>
(function(){
    var dot = document.getElementById('unreadDot');
    if(!dot) return;
    var ctx = '${pageContext.request.contextPath}';
    function tick(){
        $.get(ctx + '/notify/list', function(r){
            if(r.success){
                var n = r.data.unread || 0;
                if(n > 0){
                    dot.textContent = n > 99 ? '99+' : n;
                    dot.style.display = 'flex';
                } else {
                    dot.style.display = 'none';
                }
            }
        });
    }
    if(typeof $ !== 'undefined'){ tick(); setInterval(tick, 30000); }
})();
</script>
</c:if>