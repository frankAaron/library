package com.library.exception;

import com.library.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全局异常处理器：拦截 Controller 抛出的异常，统一返回 JSON，
 * 避免异常堆栈直接暴露给前端
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Pattern DUP_ENTRY_PATTERN = Pattern.compile("Duplicate entry '([^']+)' for key '([^']+)'");

    /** 业务异常（Service 层主动抛出的可预期业务错误） */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusiness(BusinessException e) {
        return Result.fail(e.getMessage());
    }

    /** 唯一索引冲突（重复插入分类编码 / ISBN / 用户名等）—— 返回重复值，让用户理解 */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result handleDuplicateKey(DuplicateKeyException e) {
        String sqlMsg = e.getRootCause() == null ? e.getMessage() : e.getRootCause().getMessage();
        String value = null, keyName = null;
        if (sqlMsg != null) {
            Matcher m = DUP_ENTRY_PATTERN.matcher(sqlMsg);
            if (m.find()) {
                value = m.group(1);
                keyName = m.group(2);
            }
        }
        String hint;
        if (value != null && keyName != null) {
            hint = "数据重复：" + value + " 已存在" + (keyName.toLowerCase().contains("code") ? "（编码不可重复）"
                    : keyName.toLowerCase().contains("isbn") ? "（ISBN 不可重复）"
                    : keyName.toLowerCase().contains("username") ? "（用户名不可重复）"
                    : "（唯一索引冲突）");
        } else {
            hint = "数据重复，请检查是否已存在相同记录";
        }
        log.warn("唯一索引冲突: {}", hint);
        return Result.fail(hint);
    }

    /** 参数校验异常（文件为空/格式不对/大小超限等用户操作不当） */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result handleIllegalArg(IllegalArgumentException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    /** 非法状态异常（资源不存在/状态不允许操作） */
    @ExceptionHandler(IllegalStateException.class)
    public Result handleIllegalState(IllegalStateException e) {
        log.warn("非法状态: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    /** 文件上传超出大小限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result handleMaxUpload(MaxUploadSizeExceededException e) {
        log.warn("上传文件过大: {}", e.getMessage());
        return Result.fail("上传文件大小超出限制（最大3MB）");
    }

    /** 静态资源 404（封面图丢失等）—— 降级为 warn，返回 HTTP 404 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException e) {
        log.warn("静态资源不存在: {}", e.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    /** 兜底：其他未知异常 */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统繁忙，请稍后重试");
    }
}