package com.zhulin.commen.event.listener;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/17
 * @Description: 当zookeeper的某个节点发生数据变动的时候，就会发送一个变更事件，然后由对应的监听器去捕获这些数据并做处理。
 */
public interface ZRpcListener<T> {
    void callBack(Object t);
}
