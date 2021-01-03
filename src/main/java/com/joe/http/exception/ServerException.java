package com.joe.http.exception;

import com.joe.utils.common.string.StringFormater;

import lombok.Getter;
import lombok.Setter;

/**
 * 服务器异常
 *
 * @author JoeKerouac
 * @version $Id: joe, v 0.1 2019年04月08日 19:12 JoeKerouac Exp $
 */
@Getter
@Setter
public class ServerException extends NetException {

    private static final long serialVersionUID = 4543409499814321713L;

    private static final String ERR_MSG = "请求[{0}]发生异常，异常类型：[{1}]，异常信息：[{2}]，错误：[{3}，HTTP状态：[{4}]";

    /**
     * 异常请求路径
     */
    private String path;

    /**
     * 服务端异常类型
     */
    private String exceptionClass;

    /**
     * 服务端异常消息
     */
    private String msg;

    /**
     * 错误信息
     */
    private String error;

    /**
     * HTTP状态
     */
    private int status;

    /**
     * 服务器异常
     * 
     * @param path
     *            请求路径
     * @param exceptionClass
     *            异常类
     * @param msg
     *            异常消息
     * @param error
     *            错误信息
     * @param status
     *            http状态
     */
    public ServerException(String path, String exceptionClass, String msg, String error, int status) {
        super(StringFormater.simpleFormat(ERR_MSG, path, exceptionClass, msg, error, status));
        this.path = path;
        this.exceptionClass = exceptionClass;
        this.msg = msg;
        this.error = error;
        this.status = status;
    }
}
