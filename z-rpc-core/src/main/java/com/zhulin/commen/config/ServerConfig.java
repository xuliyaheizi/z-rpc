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
    /**
     * 服务端序列化方式
     */
    private String serverSerialize;
    /**
     * 限制服务端最大所能接受的数据包体积
     */
    private Integer maxServerRequestData;
    /**
     * 服务端业务线程数目
     */
    private Integer serverBizThreadNums;
    /**
     * 服务端接收队列的大小
     */
    private Integer serverQueueSize;
    /**
     * 服务端最大连接数
     */
    private Integer maxConnections;
}
