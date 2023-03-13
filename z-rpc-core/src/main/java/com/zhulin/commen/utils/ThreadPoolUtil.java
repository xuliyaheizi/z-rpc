package com.zhulin.commen.utils;

import com.zhulin.commen.Exception.ZRpcException;

import java.util.concurrent.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/3
 * @Description: 线程池工具类
 */
public class ThreadPoolUtil {

    /**
     * 创建线程池
     *
     * @param serverType
     * @param corePoolSize
     * @param maxPoolSize
     * @return
     */
    public static ThreadPoolExecutor makeServerThreadPool(String serverType, int corePoolSize, int maxPoolSize) {
        ThreadPoolExecutor serverHandlerPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                //new LinkedBlockingQueue<Runnable>(1000),
                new SynchronousQueue<>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "z-rpc, " + serverType + "-serverHandlerPool-" + r.hashCode());
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        throw new ZRpcException("z-rpc " + serverType + " Thread pool is EXHAUSTED!");
                    }
                }
        );
        return serverHandlerPool;
    }
}
