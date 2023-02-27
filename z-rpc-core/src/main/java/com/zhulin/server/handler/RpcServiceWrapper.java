package com.zhulin.server.handler;

import lombok.Data;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/27
 * @Description: 服务注册参数包装类
 */
@Data
public class RpcServiceWrapper {
    /**
     * 对外暴露的具体服务
     */
    private Object serviceObj;
    /**
     * 服务的组名
     */
    private String group = "default";
    /**
     * 整个应用的token校验
     */
    private String serviceToken = "token-a";
    /**
     * 限流策略
     */
    private Integer limit = 10;

    public RpcServiceWrapper(Object serviceObj, String group) {
        this.serviceObj = serviceObj;
        this.group = group;
    }
}
