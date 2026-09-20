package com.library.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.Result;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 登录拦截器（一级拦截）
 * 未登录用户访问受限资源时统一拦截：
 * - Ajax 请求：返回 JSON 提示"请先登录"，由前端统一处理
 * - 页面请求：重定向到登录页
 */
public class LoginInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Object loginUser = request.getSession().getAttribute("loginUser");
        if (loginUser != null) {
            return true;
        }
        // 区分 Ajax 请求与页面请求，分别返回 JSON / 重定向
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(Result.fail("请先登录")));
        } else {
            response.sendRedirect(request.getContextPath() + "/toLogin");
        }
        return false;
    }
}