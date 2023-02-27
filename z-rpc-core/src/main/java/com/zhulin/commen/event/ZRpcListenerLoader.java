package com.zhulin.commen.event;

import com.zhulin.commen.event.handler.ZRpcEvent;
import com.zhulin.commen.event.listener.ZRpcListener;
import com.zhulin.commen.event.listener.ProviderNodeDataChangeListener;
import com.zhulin.commen.event.listener.ServiceDestroyListener;
import com.zhulin.commen.event.listener.ServiceUpdateListener;
import com.zhulin.commen.utils.CommonUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/17
 * @Description: 事件监听加载器
 */
public class ZRpcListenerLoader {

    private static final List<ZRpcListener> zRpcListenerList = new ArrayList<>();
    /**
     * 线程池
     */
    private static final ExecutorService eventThreadPool = Executors.newFixedThreadPool(2);

    public static void registerListener(ZRpcListener iRpcListener) {
        zRpcListenerList.add(iRpcListener);
    }

    public void init() {
        registerListener(new ServiceUpdateListener());
        registerListener(new ServiceDestroyListener());
        registerListener(new ProviderNodeDataChangeListener());
    }

    /**
     * 获取接口上的泛型T
     *
     * @param o 接口
     */
    public static Class<?> getInterfaceT(Object o) {
        Type[] types = o.getClass().getGenericInterfaces();
        ParameterizedType parameterizedType = (ParameterizedType) types[0];
        Type type = parameterizedType.getActualTypeArguments()[0];
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        return null;
    }

    /**
     * 同步事件处理，可能会堵塞
     *
     * @param zRpcEvent
     */
    public static void sendSyncEvent(ZRpcEvent zRpcEvent) {
        if (CommonUtil.isEmptyList(zRpcListenerList)) {
            return;
        }
        for (ZRpcListener<?> iRpcListener : zRpcListenerList) {
            // 获取listener上监听事件的泛型
            Class<?> type = getInterfaceT(iRpcListener);
            // 判断Listener监听事件的泛型是否与Watcher传递的一致
            if (type.equals(zRpcEvent.getClass())) {
                try {
                    //一致则执行回调函数
                    iRpcListener.callBack(zRpcEvent.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 异步事件处理
     *
     * @param zRpcEvent
     */
    public static void sendEvent(ZRpcEvent zRpcEvent) {
        if (zRpcListenerList.isEmpty()) {
            return;
        }
        for (ZRpcListener<?> iRpcListener : zRpcListenerList) {
            // 获取listener上监听事件的泛型
            Class<?> type = getInterfaceT(iRpcListener);
            // 判断Listener监听事件的泛型是否与Watcher传递的一致
            if (type.equals(zRpcEvent.getClass())) {
                eventThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //一致则异步执行回调函数
                            iRpcListener.callBack(zRpcEvent.getData());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }
}
