package com.zhulin.filter.client.impl;

import com.zhulin.commen.cache.CommonClientCache;
import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.filter.ZClientFilter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 客户端调用日志记录
 */
@Slf4j
public class ClientLogFilterImpl implements ZClientFilter {
    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInfoContent rpcInfoContent) {
        rpcInfoContent.getAttachments().put("c_app_name", CommonClientCache.CLIENT_CONFIG.getApplicationName());
        log.info(rpcInfoContent.getAttachments().get("c_app_name") + " do invoke ----> " + rpcInfoContent.getTargetServiceName());
    }
}
