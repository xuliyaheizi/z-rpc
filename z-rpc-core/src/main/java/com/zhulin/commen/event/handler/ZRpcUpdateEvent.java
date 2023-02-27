package com.zhulin.commen.event.handler;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/17
 * @Description:
 */
public class ZRpcUpdateEvent implements ZRpcEvent {
    private Object data;

    public ZRpcUpdateEvent(Object data) {
        this.data = data;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public ZRpcEvent setData(Object data) {
        this.data = data;
        return this;
    }
}
