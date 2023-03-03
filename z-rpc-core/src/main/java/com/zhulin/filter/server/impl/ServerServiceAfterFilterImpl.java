package com.zhulin.filter.server.impl;

import com.zhulin.commen.annotations.SPI;
import com.zhulin.commen.concurrent.ServiceSemaphoreWrapper;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.filter.ZServerFilter;

import static com.zhulin.commen.cache.CommonServerCache.SERVER_SERVICE_SEMAPHORE;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description:
 */
@SPI("after")
public class ServerServiceAfterFilterImpl implements ZServerFilter {
    @Override
    public void doFilter(RpcInfoContent rpcInfoContent) {
        String serviceName = rpcInfoContent.getTargetServiceName();
        ServiceSemaphoreWrapper serviceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE.get(serviceName);
        serviceSemaphoreWrapper.getSemaphore().release();
    }
}
