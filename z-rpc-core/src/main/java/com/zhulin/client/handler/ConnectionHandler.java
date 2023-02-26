package com.zhulin.client.handler;

import io.netty.bootstrap.Bootstrap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 处理服务的连接，断开，按照服务名筛选
 */
@Data
public class ConnectionHandler {
    /**
     * Netty的核心连接器，专门用于负责和服务构建连接通信
     */
    private Bootstrap bootstrap;
}
