package com.zhulin.registry.zookeeper.impl;

import com.alibaba.fastjson.JSON;
import com.zhulin.commen.event.ZRpcListenerLoader;
import com.zhulin.commen.event.data.URLChangeWrapper;
import com.zhulin.commen.event.handler.ZRpcEvent;
import com.zhulin.commen.event.handler.ZRpcNodeChangeEvent;
import com.zhulin.commen.event.handler.ZRpcUpdateEvent;
import com.zhulin.commen.utils.CommonUtil;
import com.zhulin.registry.AbstractRegistry;
import com.zhulin.registry.RegistryService;
import com.zhulin.registry.URL;
import com.zhulin.registry.zookeeper.AbstractZookeeperClient;
import com.zhulin.registry.zookeeper.CuratorZookeeperClient;
import com.zhulin.registry.zookeeper.ProviderNodeInfo;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zhulin.commen.cache.CommonClientCache.CLIENT_CONFIG;
import static com.zhulin.commen.cache.CommonServerCache.SERVER_CONFIG;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/26
 * @Description: zookeeper注册中心
 */
public class ZookeeperRegistry extends AbstractRegistry implements RegistryService {
    private AbstractZookeeperClient zkClient;
    private static final String ROOT = "/ZRPC";

    public ZookeeperRegistry() {
        String address = CLIENT_CONFIG != null ? CLIENT_CONFIG.getRegisterAddr() :
                SERVER_CONFIG.getRegisterAddr();
        this.zkClient = new CuratorZookeeperClient(address);
    }

    public ZookeeperRegistry(String address) {
        this.zkClient = new CuratorZookeeperClient(address);
    }

    /**
     * 获取provider的路径
     *
     * @param url
     * @return
     */
    private String getProviderPath(URL url) {
        return ROOT + "/" + url.getServiceName() + "/provider/" + url.getParameters().get("host") + ":" + url.getParameters().get("port");
    }

    /**
     * 获取consumer路径
     *
     * @param url
     * @return
     */
    private String getConsumerPath(URL url) {
        return ROOT + "/" + url.getServiceName() + "/consumer/" + url.getApplicationName() + ":" + url.getParameters().get("host") + ":";
    }

    @Override
    public void doAfterSubscribe(URL url) {
        //监听服务上线下线
        String servicePath = url.getParameters().get("servicePath");
        if (!CommonUtil.isEmpty(servicePath)) {
            // watchChildNodePath ---> /ZRPC/com.zhulin.services.UserService/provider
            String watchChildNodePath = ROOT + "/" + servicePath;
            watchChildNodeData(watchChildNodePath);
        }
        //监听服务节点数据变化
        String providerIpStrJson = url.getParameters().get("providerIps");
        if (!CommonUtil.isEmpty(providerIpStrJson)) {
            List<String> providerIpList = JSON.parseObject(providerIpStrJson, List.class);
            for (String providerIp : providerIpList) {
                //监听服务节点信息变化
                watchNodeDataChange(ROOT + "/" + servicePath + "/" + providerIp);
            }
        }
        //监听单个服务节点数据变化
        String providerPath = url.getParameters().get("providerPath");
        if (!CommonUtil.isEmpty(providerPath)) {
            watchNodeDataChange(ROOT + "/" + providerPath);
        }
    }

    /**
     * 订阅服务节点内部的数据变化
     * 监听/ZRPC/com.zhulin.services.UserService/provider/192.168.100.141:8080的数据变化
     *
     * @param watchNodeDataPath
     */
    public void watchNodeDataChange(String watchNodeDataPath) {
        zkClient.watchNodeData(watchNodeDataPath, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                //监听节点数据修改事件
                if (watchedEvent.getType() == Event.EventType.NodeDataChanged) {
                    String path = watchedEvent.getPath();
                    String nodeData = zkClient.getNodeData(path);
                    //nodeData = nodeData.replace(";", "/");
                    ProviderNodeInfo providerNodeInfo = URL.buildProviderNodeFromUrlStr(nodeData);
                    ZRpcEvent iRpcEvent = new ZRpcNodeChangeEvent(providerNodeInfo);
                    ZRpcListenerLoader.sendEvent(iRpcEvent);
                    watchNodeDataChange(watchNodeDataPath);
                }
            }
        });
    }

    /**
     * 监听节点数据
     * 监听/ZRPC/com.zhulin.services.UserService/provider下列表是否有变化
     *
     * @param watchChildNodePath
     */
    private void watchChildNodeData(String watchChildNodePath) {
        zkClient.watchChildNodeData(watchChildNodePath, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getType() == Event.EventType.NodeChildrenChanged) {
                    String path = event.getPath();
                    //如果childrenData为空,说明该服务已经没有提供者了
                    List<String> childrenData = zkClient.getChildrenData(path);
                    URLChangeWrapper urlChangeWrapper = new URLChangeWrapper();
                    urlChangeWrapper.setProviderUrl(childrenData);
                    urlChangeWrapper.setServiceName(path.split("/")[2]);
                    if (!CommonUtil.isEmptyList(childrenData)) {
                        Map<String, String> result = new HashMap<>();
                        for (String ipAndHost : childrenData) {
                            String childData = zkClient.getNodeData(path + "/" + ipAndHost);
                            result.put(ipAndHost, childData);
                        }
                        urlChangeWrapper.setNodeDataUrl(result);
                    }
                    //自定义的一套监听组件
                    ZRpcEvent iRpcEvent = new ZRpcUpdateEvent(urlChangeWrapper);
                    ZRpcListenerLoader.sendEvent(iRpcEvent);
                    //收到回调后再注册一次监听，这样能保证一直都收到信息
                    watchChildNodeData(path);
                }
            }
        });
    }

    @Override
    public void doBeforeSubscribe(URL url) {

    }

    @Override
    public List<String> getProviderIps(String serviceName) {
        List<String> nodeDataList = this.zkClient.getChildrenData(ROOT + "/" + serviceName + "/provider");
        return nodeDataList;
    }

    @Override
    public Map<String, String> getProviderNodeInfos(String serviceName) {
        List<String> nodeDataList = this.zkClient.getChildrenData(ROOT + "/" + serviceName + "/provider");
        Map<String, String> result = new HashMap<>();
        for (String ipAndHost : nodeDataList) {
            String childData = this.zkClient.getNodeData(ROOT + "/" + serviceName + "/provider/" + ipAndHost);
            result.put(ipAndHost, childData);
        }
        return result;
    }

    @Override
    public void register(URL url) {
        if (!zkClient.existNode(ROOT)) {
            //创建根节点
            zkClient.createPersistentData(ROOT, "");
        }
        String urlStr = URL.buildProviderURLStr(url);
        if (zkClient.existNode(getProviderPath(url))) {
            zkClient.deleteNode(getProviderPath(url));

        }
        //创建临时节点
        zkClient.createTemporaryData(getProviderPath(url), urlStr);
        //将信息添加到缓存集合中
        super.register(url);
    }

    @Override
    public void unRegister(URL url) {
        zkClient.deleteNode(getProviderPath(url));
        super.unRegister(url);
    }

    @Override
    public void subscriber(URL url) {
        if (!zkClient.existNode(ROOT)) {
            zkClient.createPersistentData(ROOT, "");
        }
        String urlStr = URL.buildConsumerURLStr(url);
        if (zkClient.existNode(getConsumerPath(url))) {
            zkClient.deleteNode(getConsumerPath(url));
        }
        //创建临时有序节点
        zkClient.createTemporarySeqData(getConsumerPath(url), urlStr);
        super.subscriber(url);
    }

    @Override
    public void unSubscriber(URL url) {
        zkClient.deleteNode(getConsumerPath(url));
        super.unSubscriber(url);
    }

}
