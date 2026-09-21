package com.library.dao;

import com.library.entity.DepositRefund;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface DepositRefundMapper {
    int insert(DepositRefund refund);
    List<DepositRefund> selectByUserId(@Param("userId") Long userId);
    DepositRefund selectById(@Param("id") Long id);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("adminRemark") String adminRemark, @Param("adminId") Long adminId);
    List<DepositRefund> selectPageAdmin(Map<String, Object> params);
    long countAdmin(@Param("status") Integer status);
}
