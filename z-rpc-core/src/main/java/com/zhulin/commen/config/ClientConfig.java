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
}
