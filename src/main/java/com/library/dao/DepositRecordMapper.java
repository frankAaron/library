package com.library.dao;

import com.library.entity.DepositRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 押金记录 DAO
 */
public interface DepositRecordMapper {

    /** 新增押金流水 */
    int insert(DepositRecord record);

    /** 我的押金流水 */
    List<DepositRecord> selectMy(@Param("userId") Long userId);
}
