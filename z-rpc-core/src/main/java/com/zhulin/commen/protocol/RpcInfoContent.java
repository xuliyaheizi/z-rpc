package com.zhulin.commen.protocol;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: z-rpc传输内容
 */
@Data
public class RpcInfoContent implements Serializable {

    /**
     * 请求的方法名
     */
    private String targetMethod;
    /**
     * 请求的服务的全限定名
     */
    private String targetServiceName;
    /**
     * 请求的参数
     */
    private Object[] args;
    private String uuid;
    /**
     * 服务的响应数据
     */
    private Object response;
    /**
     * 异常信息
     */
    private Throwable e;
}
