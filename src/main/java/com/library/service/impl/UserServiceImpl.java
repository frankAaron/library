package com.library.service.impl;

import com.library.common.Constants;
import com.library.common.Result;
import com.library.dao.UserMapper;
import com.library.entity.User;
import com.library.service.PermissionService;
import com.library.service.UserService;
import com.library.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户业务实现
 * <p>
 * 注册时按三级身份自动初始化差异化权限参数：
 * 学生 5本/30天/可续借1次/罚款0.5元每天；教师 10本/60天/2次/0.3元；访客 2本/15天/0次/1元
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PermissionService permissionService;

    @Override
    @Transactional
    public Result register(User user) {
        // 1. 基础信息校验
        if (isBlank(user.getUsername()) || isBlank(user.getPassword()) || isBlank(user.getRealName())) {
            return Result.fail("账号、密码、姓名均不能为空");
        }
        if (user.getUsername().trim().length() > 50) {
            return Result.fail("账号长度不能超过50个字符");
        }
        if (user.getPassword().length() < 6) {
            return Result.fail("密码长度不能少于6位");
        }
        // 2. 读者身份校验（仅允许 学生/教师/访客 三级身份注册）
        Integer role = user.getRole();
        if (role == null || role < Constants.ROLE_STUDENT || role > Constants.ROLE_VISITOR) {
            return Result.fail("请选择正确的读者身份");
        }
        // 3. 用户名唯一性校验
        if (userMapper.selectByUsername(user.getUsername().trim()) != null) {
            return Result.fail("该账号已被注册，请更换账号");
        }
        // 4. 按身份初始化差异化权限参数
        initPermByRole(user, role);
        user.setUsername(user.getUsername().trim());
        user.setPassword(MD5Util.encrypt(user.getPassword()));
        user.setDeposit(BigDecimal.ZERO);
        user.setStatus(0);
        userMapper.insert(user);
        return Result.ok("注册成功，请登录");
    }

    /** 按角色写入默认借阅权限参数 */
    private void initPermByRole(User user, Integer role) {
        switch (role) {
            case Constants.ROLE_STUDENT:
                user.setMaxBorrowCount(5);
                user.setMaxBorrowDays(30);
                user.setMaxRenewCount(1);
                user.setFinePerDay(new BigDecimal("0.50"));
                break;
            case Constants.ROLE_TEACHER:
                user.setMaxBorrowCount(10);
                user.setMaxBorrowDays(60);
                user.setMaxRenewCount(2);
                user.setFinePerDay(new BigDecimal("0.30"));
                break;
            default: // 访客
                user.setMaxBorrowCount(2);
                user.setMaxBorrowDays(15);
                user.setMaxRenewCount(0);
                user.setFinePerDay(new BigDecimal("1.00"));
        }
    }

    @Override
    public Result login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return Result.fail("请输入账号和密码");
        }
        User user = userMapper.selectByUsername(username.trim());
        // 账号不存在或密码错误统一提示，防止账号枚举
        if (user == null || !MD5Util.matches(password, user.getPassword())) {
            return Result.fail("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            return Result.fail("账号已被停用，请联系管理员");
        }
        user.setPassword(null);
        return Result.ok("登录成功", user);
    }

    @Override
    @Transactional
    public User updateProfile(User user) {
        userMapper.updateProfile(user);
        permissionService.evictUserCache(user.getId());
        User fresh = userMapper.selectById(user.getId());
        if (fresh != null) {
            fresh.setPassword(null);
        }
        return fresh;
    }

    @Override
    @Transactional
    public Result changePwd(Long userId, String oldPassword, String newPassword) {
        if (isBlank(newPassword) || newPassword.length() < 6) {
            return Result.fail("新密码长度不能少于6位");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (!MD5Util.matches(oldPassword, user.getPassword())) {
            return Result.fail("原密码错误");
        }
        userMapper.updatePassword(userId, MD5Util.encrypt(newPassword));
        permissionService.evictUserCache(userId);
        return Result.ok("密码修改成功，请牢记新密码");
    }

    @Override
    public Map<String, Object> pageReaders(String keyword, Integer role, Integer pageNum, Integer pageSize) {
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int offset = (num - 1) * size;
        List<User> list = userMapper.selectPage(keyword, role, offset, size);
        // 脱敏：不下发密码字段
        for (User u : list) {
            u.setPassword(null);
        }
        long total = userMapper.countPage(keyword, role);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pages", (total + size - 1) / size);
        result.put("pageNum", num);
        result.put("pageSize", size);
        return result;
    }

    @Override
    @Transactional
    public Result updatePerm(User user) {
        if (user.getId() == null) {
            return Result.fail("参数错误");
        }
        if (user.getMaxBorrowCount() == null || user.getMaxBorrowCount() < 0
                || user.getMaxBorrowDays() == null || user.getMaxBorrowDays() < 0
                || user.getMaxRenewCount() == null || user.getMaxRenewCount() < 0
                || user.getFinePerDay() == null || user.getFinePerDay().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail("权限参数不合法，请检查后重新提交");
        }
        userMapper.updatePerm(user);
        // 权限变更立即失效缓存，保证借阅校验实时生效
        permissionService.evictUserCache(user.getId());
        return Result.ok("权限参数已更新");
    }

    @Override
    @Transactional
    public Result updateStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail("状态参数错误");
        }
        userMapper.updateStatus(userId, status);
        permissionService.evictUserCache(userId);
        return Result.ok(status == 1 ? "账号已停用" : "账号已启用");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
