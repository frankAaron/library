package com.library.dao;

import com.library.entity.BorrowRecord;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 借阅记录 DAO
 */
public interface BorrowRecordMapper {

    /** 新增借阅记录 */
    int insert(BorrowRecord record);

    /** 按ID查询 */
    BorrowRecord selectById(@Param("id") Long id);

    /** 查询某用户对某图书的在借记录（状态0/2） */
    BorrowRecord selectBorrowing(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /** 查询某用户所有在借图书ID集合（状态0/2） */
    List<Long> selectBorrowingBookIds(@Param("userId") Long userId);

    /** 我的借阅列表（联查图书信息） */
    List<BorrowRecord> selectMy(@Param("userId") Long userId);

    /** 查询用户的到期预警记录（3天内到期或已超期） */
    List<BorrowRecord> selectDueSoon(@Param("userId") Long userId, @Param("days") int days);

    /** 在借数量（状态0/2） */
    long countBorrowing(@Param("userId") Long userId);

    /** 累计借阅总次数（动态首页活跃度判断） */
    long countTotal(@Param("userId") Long userId);

    /** 最近 N 天借阅次数（活跃读者判定，更精准） */
    long countRecent(@Param("userId") Long userId, @Param("days") int days);

    /** 管理端分页查询借阅记录 */
    List<BorrowRecord> selectPageAdmin(@Param("keyword") String keyword,
                                       @Param("status") Integer status,
                                       @Param("startDate") String startDate,
                                       @Param("endDate") String endDate,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    /** 管理端查询总数 */
    long countAdmin(@Param("keyword") String keyword,
                    @Param("status") Integer status,
                    @Param("startDate") String startDate,
                    @Param("endDate") String endDate);

    /** 定时任务：将已过应还日期的"借阅中"记录置为超期状态 */
    int markOverdue();

    /** 续借：顺延应还日期并累加续借次数 */
    int markRenew(@Param("id") Long id, @Param("dueDate") Date dueDate);

    /** 归还：更新状态、归还时间与罚款金额 */
    int markReturned(@Param("id") Long id,
                     @Param("returnDate") Date returnDate,
                     @Param("fineAmount") BigDecimal fineAmount);

    /** 推荐算法：查询用户近 N 天借阅记录（联查图书分类） */
    List<BorrowRecord> selectRecentWithBook(@Param("userId") Long userId,
                                            @Param("days") int days,
                                            @Param("limit") int limit);

    /** 全部在借数量（统计页） */
    long countAllBorrowing();

    /** 超期记录数量（统计页） */
    long countOverdue();

    /** 全部临期记录数量（到期预警任务日志） */
    long countDueSoonAll(@Param("days") int days);

    /** 定时任务：按用户分组统计临期借阅数量，用于批量发送到期提醒 */
    List<Map<String, Object>> countDueSoonByUser(@Param("days") int days);

    /** 定时任务：单本超期 ≥ days 天的用户，自动冻结 status=1，返回影响行数 */
    int freezeByOverdueDays(@Param("days") int days);

    /** 定时任务：查询所有当前有超期在借且未冻结的用户及其超期应缴金额 */
    List<Map<String, Object>> listOverdueUserTotal();

    /** 缴清罚款后自动解冻检查：用户是否仍存在超期≥days天未归还的记录 */
    long countLongOverdueByUser(@Param("userId") Long userId, @Param("days") int days);
}