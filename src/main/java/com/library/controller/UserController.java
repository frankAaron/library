package com.library.controller;

import com.library.common.Result;
import com.library.entity.User;
import com.library.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

/**
 * 用户控制器（注册/登录/退出/资料/密码）
 * <p>
 * 登录成功后将用户信息写入 Session（key=loginUser），
 * 后续请求由 LoginInterceptor / RoleInterceptor 统一进行权限拦截。
 */
@Controller
public class UserController {

    /** Session 中登录用户的键名 */
    public static final String SESSION_USER = "loginUser";

    @Autowired
    private UserService userService;

    /** 分身份注册（学生/教师/访客） */
    @PostMapping("/user/register")
    @ResponseBody
    public Result register(@RequestBody User user) {
        return userService.register(user);
    }

    /** 登录 */
    @PostMapping("/user/login")
    @ResponseBody
    public Result login(@RequestBody Map<String, String> param, HttpSession session) {
        Result result = userService.login(param.get("username"), param.get("password"));
        if (result.isSuccess()) {
            session.setAttribute(SESSION_USER, result.getData());
        }
        return result;
    }

    /** 退出登录 */
    @GetMapping("/user/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/index";
    }

    /** 修改个人资料（姓名/手机/邮箱） */
    @PostMapping("/user/update")
    @ResponseBody
    public Result update(@RequestBody User param, HttpSession session) {
        User loginUser = (User) session.getAttribute(SESSION_USER);
        // 仅允许修改本人资料，且仅开放姓名/手机/邮箱三个字段
        User user = new User();
        user.setId(loginUser.getId());
        user.setRealName(param.getRealName());
        user.setPhone(param.getPhone());
        user.setEmail(param.getEmail());
        User fresh = userService.updateProfile(user);
        // 同步刷新 Session 中的用户信息
        if (fresh != null) {
            session.setAttribute(SESSION_USER, fresh);
        }
        return Result.ok("资料更新成功", fresh);
    }

    /** 修改密码 */
    @PostMapping("/user/changePwd")
    @ResponseBody
    public Result changePwd(@RequestBody Map<String, String> param, HttpSession session) {
        User loginUser = (User) session.getAttribute(SESSION_USER);
        return userService.changePwd(loginUser.getId(),
                param.get("oldPassword"), param.get("newPassword"));
    }
}