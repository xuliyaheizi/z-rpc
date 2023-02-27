package com.zhulin.commen.config;

import lombok.Data;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 服务配置类
 */
@Data
public class ServerConfig {
    /**
     * 服务端口号
     */
    private Integer serverPort;
    /**
     * 服务名
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
