package com.library.service.impl;

import com.library.common.Constants;
import com.library.common.Result;
import com.library.dao.BookMapper;
import com.library.dao.BorrowRecordMapper;
import com.library.dao.BrowseHistoryMapper;
import com.library.dao.FineRecordMapper;
import com.library.dao.ReservationMapper;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.BrowseHistory;
import com.library.entity.FineRecord;
import com.library.entity.Reservation;
import com.library.entity.User;
import com.library.service.BookService;
import com.library.service.BorrowService;
import com.library.service.NotificationService;
import com.library.service.PermissionService;
import com.library.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 借阅业务实现（全流程自动化核心）
 * <p>
 * 借阅：权限校验（账号/罚款/押金/重复/额度/库存）→ 条件扣库存（防并发超卖）
 *       → 生成借阅记录并按身份自动计算应还日期；
 * 归还：超期自动计算罚款并生成罚款单 → 回补库存 → 通知排队预订读者；
 * 续借：校验身份续借规则后应还日期顺延一个借阅期；
 * 超期：定时任务每日自动标记，缴费后解除借阅限制。
 */
@Service
public class BorrowServiceImpl implements BorrowService {

    private static final Logger log = LoggerFactory.getLogger(BorrowServiceImpl.class);

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private FineRecordMapper fineRecordMapper;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BookService bookService;

    @Override
    @Transactional
    public Result borrow(User sessionUser, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return Result.fail("图书不存在");
        }
        // 取最新权限信息（缓存，含押金余额）
        User user = permissionService.getUserCached(sessionUser.getId());
        // 1. 借阅前置校验（六级校验链）
        Result check = permissionService.checkBorrow(user, book);
        if (!check.isSuccess()) {
            return check;
        }
        // 2. 条件扣库存（UPDATE ... WHERE stock>0，防止并发超卖）
        if (bookMapper.deductStock(book.getId()) == 0) {
            return Result.fail("库存不足，您可以预订此书");
        }
        // 3. 生成借阅记录，按身份自动计算应还日期
        Date now = new Date();
        BorrowRecord record = new BorrowRecord();
        record.setUserId(user.getId());
        record.setBookId(book.getId());
        record.setBorrowDate(now);
        record.setDueDate(DateUtil.addDays(now, user.getMaxBorrowDays()));
        record.setStatus(Constants.RECORD_BORROWING);
        record.setRenewCount(0);
        record.setFineAmount(BigDecimal.ZERO);
        borrowRecordMapper.insert(record);
        // 4. 累计借阅热度（推荐算法与热门榜单数据源），并失效热榜缓存
        bookMapper.incrBorrowCount(book.getId());
        bookService.evictHot();
        return Result.ok("借阅成功！应还日期：" + DateUtil.fmtDate(record.getDueDate())
                + "，请按时归还");
    }

    @Override
    @Transactional
    public Result returnBook(User sessionUser, Long recordId) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            return Result.fail("借阅记录不存在");
        }
        if (!record.getUserId().equals(sessionUser.getId())) {
            return Result.fail("无权操作他人的借阅记录");
        }
        if (record.getStatus() != Constants.RECORD_BORROWING
                && record.getStatus() != Constants.RECORD_OVERDUE) {
            return Result.fail("该图书已归还，请勿重复操作");
        }
        User user = permissionService.getUserCached(sessionUser.getId());
        Date now = new Date();
        boolean overdue = record.getStatus() == Constants.RECORD_OVERDUE
                || (record.getDueDate() != null && now.after(record.getDueDate()));
        BigDecimal fine = BigDecimal.ZERO;
        int overdueDays = 0;
        // 1. 超期自动计费：罚款 = 超期天数(向上取整) × 身份日罚款标准
        if (overdue) {
            overdueDays = DateUtil.diffDaysCeil(now, record.getDueDate());
            fine = user.getFinePerDay().multiply(BigDecimal.valueOf(overdueDays))
                    .setScale(2, RoundingMode.HALF_UP);
            FineRecord fineRecord = new FineRecord();
            fineRecord.setBorrowRecordId(record.getId());
            fineRecord.setUserId(record.getUserId());
            fineRecord.setAmount(fine);
            fineRecordMapper.insert(fineRecord);
        }
        // 2. 更新借阅记录（有罚款置为"超期归还"，否则"正常归还"）
        borrowRecordMapper.markReturned(record.getId(), now, fine);
        // 3. 回补库存
        bookMapper.restoreStock(record.getBookId());
        // 4. 如果有排队预订的第一位读者 → 自动分配（库存立即再扣 1，净效果不变）
        Reservation firstWaiting = reservationMapper.selectFirstWaitingWithBook(record.getBookId());
        String msg;
        if (firstWaiting != null) {
            User nextUser = permissionService.getUserCached(firstWaiting.getUserId());
            if (nextUser != null
                    && nextUser.getStatus() == 0
                    && borrowRecordMapper.countBorrowing(nextUser.getId()) < nextUser.getMaxBorrowCount()) {
                // 给预订读者立即扣库存 + 创建借阅记录
                if (bookMapper.deductStock(record.getBookId()) > 0) {
                    Book autoBook = bookMapper.selectById(record.getBookId());
                    BorrowRecord autoRecord = new BorrowRecord();
                    autoRecord.setUserId(nextUser.getId());
                    autoRecord.setBookId(record.getBookId());
                    autoRecord.setBookName(autoBook != null ? autoBook.getBookName() : null);
                    autoRecord.setBorrowDate(now);
                    autoRecord.setDueDate(DateUtil.addDays(now, nextUser.getMaxBorrowDays()));
                    autoRecord.setStatus(Constants.RECORD_BORROWING);
                    autoRecord.setRenewCount(0);
                    autoRecord.setFineAmount(BigDecimal.ZERO);
                    borrowRecordMapper.insert(autoRecord);
                    // 预订状态置为"已完成"
                    reservationMapper.finish(firstWaiting.getId());
                    bookMapper.incrBorrowCount(record.getBookId());
                    bookService.evictHot();
                    // 给预订读者发站内消息 + 邮件
                    String bookTitle = firstWaiting.getBookName() == null ? "该图书" : "《" + firstWaiting.getBookName() + "》";
                    try {
                        notificationService.send(nextUser.getId(), Constants.NOTIFY_RESERVE_READY,
                                "您预订的图书已自动借阅",
                                bookTitle + " 已自动借阅成功！应还日期："
                                        + DateUtil.fmtDate(autoRecord.getDueDate()));
                    } catch (Exception e) {
                        log.warn("预订自动分配通知发送失败: {}", e.getMessage());
                    }
                } else {
                    // 理论上 restoreStock 后库存必 ≥1，这里保底
                    reservationMapper.notifyFirst(record.getBookId());
                }
            } else {
                // 预订读者账号有问题（停用/额度满/冻结），降级为仅通知
                reservationMapper.notifyFirst(record.getBookId());
            }
        } else {
            firstWaiting = null;
        }
        if (overdue) {
            msg = "归还成功！该书已超期" + overdueDays + "天，产生罚款" + fine.stripTrailingZeros().toPlainString()
                    + "元，请前往「我的借阅」缴纳罚款，未缴清前无法继续借阅";
            if (fine.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    notificationService.send(record.getUserId(), Constants.NOTIFY_FINE_GENERATED,
                            "您有新的超期罚款待缴",
                            "图书《" + (record.getBookName() == null ? "" : record.getBookName())
                                    + "》超期归还，罚款金额 " + fine.stripTrailingZeros().toPlainString()
                                    + " 元，请尽快前往「我的借阅」缴纳。");
                } catch (Exception e) {
                    log.warn("罚款通知消息发送失败: {}", e.getMessage());
                }
            }
        } else {
            msg = "归还成功！感谢按时归还";
        }
        return Result.ok(msg);
    }

    @Override
    @Transactional
    public Result renew(User sessionUser, Long recordId) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            return Result.fail("借阅记录不存在");
        }
        if (!record.getUserId().equals(sessionUser.getId())) {
            return Result.fail("无权操作他人的借阅记录");
        }
        User user = permissionService.getUserCached(sessionUser.getId());
        // 续借规则校验（状态/超期/身份规则/剩余次数）
        Result check = permissionService.checkRenew(user, record);
        if (!check.isSuccess()) {
            return check;
        }
        // 应还日期顺延一个借阅期
        Date newDueDate = DateUtil.addDays(record.getDueDate(), user.getMaxBorrowDays());
        if (borrowRecordMapper.markRenew(record.getId(), newDueDate) == 0) {
            return Result.fail("续借失败，请稍后重试");
        }
        return Result.ok("续借成功！新应还日期：" + DateUtil.fmtDate(newDueDate));
    }

    @Override
    @Transactional
    public Result reserve(User sessionUser, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return Result.fail("图书不存在");
        }
        // 有库存无需预订
        if (book.getStock() != null && book.getStock() > 0) {
            return Result.fail("该书有库存，可直接借阅");
        }
        // 不可重复预订
        if (reservationMapper.existsActive(sessionUser.getId(), bookId) > 0) {
            return Result.fail("您已预订此书，请耐心等待到书通知");
        }
        Reservation reservation = new Reservation();
        reservation.setUserId(sessionUser.getId());
        reservation.setBookId(bookId);
        reservation.setReserveTime(new Date());
        reservation.setStatus(Constants.RESERVE_WAITING);
        reservationMapper.insert(reservation);
        return Result.ok("预订成功！图书回库后将按预订顺序通知您");
    }

    @Override
    @Transactional
    public Result cancelReserve(User sessionUser, Long reserveId) {
        if (reservationMapper.cancel(reserveId, sessionUser.getId()) == 0) {
            return Result.fail("预订不存在或当前状态不可取消");
        }
        return Result.ok("预订已取消");
    }

    @Override
    public List<BorrowRecord> myBorrows(Long userId) {
        return borrowRecordMapper.selectMy(userId);
    }

    @Override
    public List<Reservation> myReservations(Long userId) {
        return reservationMapper.selectMy(userId);
    }

    @Override
    public List<BorrowRecord> dueSoon(Long userId) {
        return borrowRecordMapper.selectDueSoon(userId, Constants.DUE_WARN_DAYS);
    }

    @Override
    public int markOverdue() {
        return borrowRecordMapper.markOverdue();
    }

    @Override
    public void recordBrowse(Long userId, Long bookId) {
        try {
            BrowseHistory history = new BrowseHistory();
            history.setUserId(userId);
            history.setBookId(bookId);
            history.setBrowseTime(new Date());
            browseHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("记录浏览历史失败, userId={}, bookId={}", userId, bookId, e);
        }
    }

    @Override
    public BorrowRecord currentBorrowing(Long userId, Long bookId) {
        if (userId == null || bookId == null) return null;
        return borrowRecordMapper.selectBorrowing(userId, bookId);
    }

    @Override
    public boolean currentReserving(Long userId, Long bookId) {
        if (userId == null || bookId == null) return false;
        return reservationMapper.existsActive(userId, bookId) > 0;
    }

    @Override
    public Set<Long> myBorrowingBookIds(Long userId) {
        if (userId == null) return new HashSet<>();
        return new HashSet<>(borrowRecordMapper.selectBorrowingBookIds(userId));
    }

    @Override
    public Set<Long> myReservingBookIds(Long userId) {
        if (userId == null) return new HashSet<>();
        return new HashSet<>(reservationMapper.selectActiveBookIds(userId));
    }

    @Override
    public Map<String, Object> pageAdmin(String keyword, Integer status, String startDate, String endDate,
                                         Integer pageNum, Integer pageSize) {
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int offset = (num - 1) * size;
        List<BorrowRecord> list = borrowRecordMapper.selectPageAdmin(
                trimToNull(keyword), status, trimToNull(startDate), trimToNull(endDate), offset, size);
        long total = borrowRecordMapper.countAdmin(
                trimToNull(keyword), status, trimToNull(startDate), trimToNull(endDate));
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pages", (total + size - 1) / size);
        result.put("pageNum", num);
        result.put("pageSize", size);
        return result;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}