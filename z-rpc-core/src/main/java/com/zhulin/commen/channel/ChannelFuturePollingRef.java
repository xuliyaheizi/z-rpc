package com.zhulin.commen.channel;

import com.zhulin.router.Selector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/27
 * @Description: 实现轮训效果，本质就是通过取模计算
 */
public class ChannelFuturePollingRef {
    private Map<String, AtomicLong> referenceMap = new ConcurrentHashMap<>();

    public ChannelFutureWrapper getChannelFutureWrapper(Selector selector) {
        AtomicLong referCount = referenceMap.get(selector.getProviderServiceName());
        if (referCount == null) {
            referCount = new AtomicLong(0);
            referenceMap.put(selector.getProviderServiceName(), referCount);
        }
        ChannelFutureWrapper[] arr = selector.getChannelFutureWrappers();
        long i = referCount.getAndIncrement();
        //通过取模计算进行轮训
        int index = (int) (i % arr.length);
        return arr[index];
    }
}
