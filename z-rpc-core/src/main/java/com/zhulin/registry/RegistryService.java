package com.zhulin.registry;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/26
 * @Description: 注册中心接口
 */
public interface RegistryService {

    /**
     * 服务注册
     *
     * @param url
     */
    void register(URL url);

    /**
     * 服务下线
     *
     * @param url
     */
    void unRegister(URL url);

    /**
     * 消费者订阅服务
     *
     * @param url
     */
    void subscriber(URL url);

    /**
     * 消费者取消订阅服务
     *
     * @param url
     */
    void unSubscriber(URL url);
}
