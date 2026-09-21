package com.library.dao;

import com.library.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户表 DAO
 */
public interface UserMapper {

    /** 新增用户（注册），回填自增ID */
    int insert(User user);

    /** 按用户名查询 */
    User selectByUsername(@Param("username") String username);

    /** 按ID查询 */
    User selectById(@Param("id") Long id);

    /** 分页查询（管理端读者管理） */
    List<User> selectPage(@Param("keyword") String keyword,
                          @Param("role") Integer role,
                          @Param("offset") int offset,
                          @Param("size") int size);

    /** 分页总数 */
    long countPage(@Param("keyword") String keyword,
                   @Param("role") Integer role);

    /** 修改个人资料（姓名/手机/邮箱） */
    int updateProfile(User user);

    /** 修改密码 */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 启用/停用账号 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 调整三级权限参数（额度/时长/续借次数/罚款标准） */
    int updatePerm(User user);

    /** 原子增减押金余额（delta 可为负） */
    int updateDeposit(@Param("id") Long id, @Param("delta") java.math.BigDecimal delta);

    /** 统计读者数量（role>0，管理端统计） */
    long countReaders();

    /** 按角色批量调整权限参数（管理员一键批量更新） */
    int batchUpdatePermByRole(@Param("role") Integer role,
                              @Param("maxBorrowCount") Integer maxBorrowCount,
                              @Param("maxBorrowDays") Integer maxBorrowDays,
                              @Param("maxRenewCount") Integer maxRenewCount,
                              @Param("finePerDay") java.math.BigDecimal finePerDay);

    /** 查询指定前缀的最大 stu_or_job_no，用于自动生成学号/工号 */
    String selectMaxStuNoByPrefix(@Param("prefix") String prefix);
}