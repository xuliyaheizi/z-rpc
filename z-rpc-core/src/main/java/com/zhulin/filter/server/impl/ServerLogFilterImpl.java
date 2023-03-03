package com.zhulin.filter.server.impl;

import com.zhulin.commen.annotations.SPI;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.filter.ZServerFilter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 服务端调用日志记录
 */
@Slf4j
@SPI("before")
public class ServerLogFilterImpl implements ZServerFilter {
    @Override
    public void doFilter(RpcInfoContent rpcInfoContent) {
        log.info(rpcInfoContent.getAttachments().get("c_app_name") + " do invoke ----> " + rpcInfoContent.getTargetServiceName() + "#" + rpcInfoContent.getTargetMethod());
    }
}
