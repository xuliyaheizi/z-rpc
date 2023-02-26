package com.zhulin.concurrent;

import com.zhulin.commen.protocol.RpcInfoContent;
import lombok.Data;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 并发中超时机制
 */
@Data
public class TimeoutInvocation {
    private final CountDownLatch countDownLatch;
    private RpcInfoContent rpcInfoContent;

    public TimeoutInvocation(RpcInfoContent rpcInfoContent) {
        this.countDownLatch = new CountDownLatch(1);
        this.rpcInfoContent = rpcInfoContent;
    }

    public Boolean tryAcquire(long timeOut, TimeUnit timeUnit) throws InterruptedException {
        return countDownLatch.await(timeOut, timeUnit);
    }

    public void release() {
        countDownLatch.countDown();
    }
}
