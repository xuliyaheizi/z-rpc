package com.zhulin.registry;

import com.zhulin.commen.cache.CommonClientCache;
import com.zhulin.commen.cache.CommonServerCache;

import java.util.List;
import java.util.Map;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/26
 * @Description: 对一些注册数据做统一的处理，假设日后需要考虑支持多种类型的注册中心，例如redis 、 etcd之类的话，所有基础的记录操作都可以统一放在抽象类里实现。
 */
public abstract class AbstractRegistry implements RegistryService {
    @Override
    public void register(URL url) {
        CommonServerCache.PROVIDER_URL_SET.add(url);
    }

    @Override
    public void unRegister(URL url) {
        CommonServerCache.PROVIDER_URL_SET.remove(url);
    }

    @Override
    public void subscriber(URL url) {
        CommonClientCache.SUBSCRIBER_SERVICE_LIST.add(url);
    }

    @Override
    public void unSubscriber(URL url) {
        CommonClientCache.SUBSCRIBER_SERVICE_LIST.remove(url);
    }

    /**
     * 留给子类扩展
     *
     * @param url
     */
    public abstract void doAfterSubscribe(URL url);

    /**
     * 留给子类扩展
     *
     * @param url
     */
    public abstract void doBeforeSubscribe(URL url);

    /**
     * 留给子类扩展
     *
     * @param serviceName
     * @return
     */
    public abstract List<String> getProviderIps(String serviceName);

    /**
     * 获取服务的节点信息
     *
     * @param serviceName
     * @return <key=ip:port --> value=urlString>,<ip:port --> urlString>,<ip:port --> urlString>,<ip:port --> urlString>
     */
    public abstract Map<String, String> getProviderNodeInfos(String serviceName);
}
