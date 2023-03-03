package com.zhulin.commen.Exception;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/2
 * @Description: 限流异常
 */
public class MaxLimitException extends ZRpcException {
    public MaxLimitException(String message) {
        super(message);
    }
}
