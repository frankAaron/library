package com.library.service;

import com.library.common.Result;
import com.library.entity.User;

import java.util.Map;

/**
 * 用户业务接口（注册/登录/资料/密码/读者管理）
 */
public interface UserService {

    /** 分身份注册（按角色初始化差异化权限参数） */
    Result register(User user);

    /** 登录校验，成功返回不含密码的用户信息 */
    Result login(String username, String password);

    /** 修改个人资料（姓名/手机/邮箱），返回更新后的用户信息 */
    User updateProfile(User user);

    /** 修改密码 */
    Result changePwd(Long userId, String oldPassword, String newPassword);

    /** 管理端：读者账号分页查询 */
    Map<String, Object> pageReaders(String keyword, Integer role, Integer pageNum, Integer pageSize);

    /** 管理端：调整读者权限参数（额度/时长/续借次数/罚款标准） */
    Result updatePerm(User user);

    /** 管理端：启用/停用读者账号 */
    Result updateStatus(Long userId, Integer status);

    /** 管理端：按角色批量调整权限参数 */
    Result batchUpdatePermByRole(Integer role, Integer maxBorrowCount, Integer maxBorrowDays,
                                 Integer maxRenewCount, java.math.BigDecimal finePerDay);
}