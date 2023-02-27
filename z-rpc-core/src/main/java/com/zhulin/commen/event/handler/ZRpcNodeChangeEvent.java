package com.zhulin.commen.event.handler;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/19
 * @Description:
 */
public class ZRpcNodeChangeEvent implements ZRpcEvent {
    private Object data;

    public ZRpcNodeChangeEvent(Object data) {
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
