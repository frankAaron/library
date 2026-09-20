package com.library.exception;

import com.library.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器：拦截 Controller 抛出的异常，统一返回 JSON，
 * 避免异常堆栈直接暴露给前端
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常（Service 层主动抛出的可预期业务错误） */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusiness(BusinessException e) {
        return Result.fail(e.getMessage());
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

    /** 兜底：其他未知异常 */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统繁忙，请稍后重试");
    }
}