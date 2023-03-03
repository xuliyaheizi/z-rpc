package com.zhulin.commen.Exception;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 自定义异常类
 */
public class ZRpcException extends RuntimeException {
    public ZRpcException(String message) {
        super(message);
    }
}
