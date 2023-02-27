package com.zhulin.server.handler;

import com.zhulin.commen.event.handler.ZRpcDestroyEvent;
import com.zhulin.commen.event.ZRpcListenerLoader;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/26
 * @Description: 监听Java进程被关闭
 */
@Slf4j
public class ApplicationShutDownHook {

    public static void registryShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("[registryShutdownHook] ======= Server Destroy ======");
                ZRpcListenerLoader.sendSyncEvent(new ZRpcDestroyEvent("destroy"));
            }
        }, "serverDestroyTask"));
    }
}
