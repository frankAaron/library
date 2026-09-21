package com.library.dao;

import com.library.entity.FineRecord;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 罚款记录 DAO
 */
public interface FineRecordMapper {

    /** 新增罚款记录 */
    int insert(FineRecord fineRecord);

    /** 按ID查询（联查图书名称，读者缴费前校验使用） */
    FineRecord selectById(@Param("id") Long id);

    /** 我的罚款列表（联查图书名称） */
    List<FineRecord> selectMy(@Param("userId") Long userId);

    /** 管理端分页查询（罚款对账） */
    List<FineRecord> selectPageAdmin(@Param("status") Integer status,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    /** 管理端查询总数 */
    long countAdmin(@Param("status") Integer status);

    /** 读者缴费（仅本人未缴记录） */
    int pay(@Param("id") Long id, @Param("userId") Long userId);

    /** 管理员人工核销（对账场景） */
    int markPaidByAdmin(@Param("id") Long id, @Param("operatorId") Long operatorId);

    /** 未缴罚款总额（统计页） */
    BigDecimal sumUnpaid();

    /** 统计用户未缴罚款笔数（借阅校验使用） */
    long countUnpaidByUser(@Param("userId") Long userId);

    /** 统计用户未缴罚款总额（冻结阈值判断） */
    BigDecimal sumUnpaidByUser(@Param("userId") Long userId);

    /** 管理员批量人工核销（按ID列表） */
    int batchMarkPaid(@Param("ids") List<Long> ids, @Param("operatorId") Long operatorId);
}