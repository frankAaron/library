package com.library.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.Constants;
import com.library.common.Result;
import com.library.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 管理员角色拦截器（二级拦截）
 * 拦截 /admin/** 全部请求，防止读者越权访问后台管理功能
 */
public class RoleInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Object obj = request.getSession().getAttribute("loginUser");
        if (obj instanceof User
                && ((User) obj).getRole() != null
                && ((User) obj).getRole() == Constants.ROLE_ADMIN) {
            return true;
        }
        // Ajax 请求返回 JSON，页面请求跳回首页
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                    Result.fail("无权限访问，该功能仅管理员可用")));
        } else {
            response.sendRedirect(request.getContextPath() + "/index");
        }
        return false;
    }
}