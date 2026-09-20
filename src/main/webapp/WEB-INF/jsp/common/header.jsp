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
