package com.zhulin.commen.event.listener;

import com.zhulin.commen.event.handler.ZRpcDestroyEvent;
import com.zhulin.registry.URL;

import static com.zhulin.commen.cache.CommonServerCache.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/19
 * @Description: 服务注销监听器
 */
public class ServiceDestroyListener implements ZRpcListener<ZRpcDestroyEvent> {

    @Override
    public void callBack(Object t) {
        for (URL url : PROVIDER_URL_SET) {
            //注销服务
            REGISTRY_SERVICE.unRegister(url);
        }
    }
}
