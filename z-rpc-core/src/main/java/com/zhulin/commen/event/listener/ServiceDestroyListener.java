package com.zhulin.commen.event.listener;

import com.zhulin.commen.event.handler.ZRpcDestroyEvent;
import com.zhulin.registry.URL;

import static com.zhulin.commen.cache.CommonServerCache.PROVIDER_URL_SET;
import static com.zhulin.commen.cache.CommonServerCache.REGISTRY_SERVICE;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/19
 * @Description: 服务注销监听器
 */
public class ServiceDestroyListener implements ZRpcListener<ZRpcDestroyEvent> {

    @Override
    public void callBack(Object t) {
        //服务端注销
        for (URL url : PROVIDER_URL_SET) {
            //注销服务
            REGISTRY_SERVICE.unRegister(url);
        }
    }
}
