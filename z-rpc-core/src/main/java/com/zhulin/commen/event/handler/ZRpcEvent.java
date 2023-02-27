package com.zhulin.commen.event.handler;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/17
 * @Description: 装载需要传递的数据消息
 */
public interface ZRpcEvent {
    Object getData();

    ZRpcEvent setData(Object data);
}
