package com.library.controller;

import com.library.common.Result;
import com.library.entity.User;
import com.library.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端-读者账号管理接口
 * 支持三级身份权限参数（借阅额度/时长/续借次数/罚款标准）的精细化调整
 */
@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    /** 读者分页查询（关键词 + 角色筛选） */
    @GetMapping("/list")
    public Result list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "role", required = false) Integer role,
                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.ok(userService.pageReaders(keyword, role, pageNum, pageSize));
    }

    /** 调整读者权限参数 */
    @PostMapping("/updatePerm")
    public Result updatePerm(@RequestBody User user) {
        return userService.updatePerm(user);
    }

    /** 启用/停用读者账号 */
    @PostMapping("/updateStatus")
    public Result updateStatus(@RequestBody Map<String, Long> param) {
        Long id = param.get("id");
        Long status = param.get("status");
        return userService.updateStatus(id, status == null ? null : status.intValue());
    }
}