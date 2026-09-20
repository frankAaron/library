package com.library.service;

import com.library.common.Result;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.Reservation;
import com.library.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 借阅业务接口（借阅/归还/续借/预订 全流程闭环）
 */
public interface BorrowService {

    /** 线上借阅：权限校验 → 扣库存 → 生成记录 → 自动计算应还日期 */
    Result borrow(User user, Long bookId);

    /** 归还：超期自动计费生成罚款单 → 回补库存 → 通知排队预订读者 */
    Result returnBook(User user, Long recordId);

    /** 一键续借：应还日期顺延一个借阅期 */
    Result renew(User user, Long recordId);

    /** 预订（库存不足时可预订，回库后按顺序通知） */
    Result reserve(User user, Long bookId);

    /** 取消预订 */
    Result cancelReserve(User user, Long reserveId);

    /** 我的借阅列表 */
    List<BorrowRecord> myBorrows(Long userId);

    /** 我的预订列表 */
    List<Reservation> myReservations(Long userId);

    /** 到期预警记录（含已超期） */
    List<BorrowRecord> dueSoon(Long userId);

    /** 执行超期标记（定时任务/管理端手动触发） */
    int markOverdue();

    /** 记录浏览历史（个性化推荐数据源，失败不影响主流程） */
    void recordBrowse(Long userId, Long bookId);

    /** 管理端：借阅记录分页查询 */
    Map<String, Object> pageAdmin(String keyword, Integer status, String startDate, String endDate,
                                  Integer pageNum, Integer pageSize);
}
