package com.library.common;

/**
 * 统一接口响应结果封装
 */
public class Result {

    /** 状态码：200成功 500失败 */
    private int code;
    /** 提示消息 */
    private String msg;
    /** 响应数据 */
    private Object data;

    public Result() {
    }

    public Result(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static Result ok() {
        return new Result(200, "操作成功", null);
    }

    public static Result ok(Object data) {
        return new Result(200, "操作成功", data);
    }

    public static Result ok(String msg, Object data) {
        return new Result(200, msg, data);
    }

    public static Result fail(String msg) {
        return new Result(500, msg, null);
    }

    /** 400 参数校验失败 */
    public static Result badRequest(String msg) {
        return new Result(400, msg, null);
    }

    /** 403 无权限 */
    public static Result forbidden(String msg) {
        return new Result(403, msg, null);
    }

    /** 404 资源不存在 */
    public static Result notFound(String msg) {
        return new Result(404, msg, null);
    }

    /** 409 业务冲突 */
    public static Result conflict(String msg) {
        return new Result(409, msg, null);
    }

    /** 自定义状态码 */
    public static Result of(int code, String msg) {
        return new Result(code, msg, null);
    }

    public boolean isSuccess() {
        return code == 200;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}