package com.zhulin.filter.server.impl;

import com.zhulin.commen.Exception.MaxLimitException;
import com.zhulin.commen.annotations.SPI;
import com.zhulin.commen.concurrent.ServiceSemaphoreWrapper;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.filter.ZServerFilter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;

import static com.zhulin.commen.cache.CommonServerCache.SERVER_SERVICE_SEMAPHORE;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description:
 */
@Slf4j
@SPI("before")
public class ServerServiceBeforeFilterImpl implements ZServerFilter {
    @Override
    public void doFilter(RpcInfoContent rpcInfoContent) {
        String serviceName = rpcInfoContent.getTargetServiceName();
        ServiceSemaphoreWrapper serviceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE.get(serviceName);
        //从缓存中提取semaphore对象
        Semaphore semaphore = serviceSemaphoreWrapper.getSemaphore();
        boolean tryResult = semaphore.tryAcquire();
        if (!tryResult) {
            String errorMsg =
                    rpcInfoContent.getTargetServiceName() + "'s max request is " + serviceSemaphoreWrapper.getMaxNums() + ",reject now";
            throw new MaxLimitException(errorMsg);
        }
    }
}
