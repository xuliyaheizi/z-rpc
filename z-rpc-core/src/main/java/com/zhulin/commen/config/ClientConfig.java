package com.zhulin.commen.config;

import lombok.Data;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 客户端配置类
 */
@Data
public class ClientConfig {
    /**
     * 应用名称
     */
    private String applicationName;

    /**
     * 注册中心地址
     */
    private String registerAddr;
    /**
     * 注册中心类型
     */
    private String registerType;
    /**
     * 代理类型 example: jdk,javassist
     */
    private String proxyType;
    /**
     * 负载均衡策略 example:random,rotate
     */
    private String routerStrategy;
    /**
     * 客户端序列化方式，fastJson、jdk、hessian、kryo
     */
    private String clientSerialize;
    /**
     * 客户端发送数据的超时时间
     */
    private Integer timeOut;
    /**
     * 客户端最大响应数据体积
     */
    private Integer maxServerRespDataSize;
}
