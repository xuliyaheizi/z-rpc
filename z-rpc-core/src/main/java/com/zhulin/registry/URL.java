package com.zhulin.registry;

import com.zhulin.registry.zookeeper.ProviderNodeInfo;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/26
 * @Description: 注册中心信息类
 */
@Data
public class URL {
    /**
     * 应用名称
     */
    private String applicationName;
    /**
     * 服务名称
     */
    private String serviceName;
    /**
     * 自定义不限进行扩展
     * 分组
     * 权重
     * 服务提供者的地址
     * 服务提供者的端口
     */
    private Map<String, String> parameters = new HashMap<>();

    public void addParameter(String key, String value) {
        this.parameters.putIfAbsent(key, value);
    }

    /**
     * 将URL转换为写入zk的provider节点下的一段字符串
     *
     * @param url
     * @return
     */
    public static String buildProviderURLStr(URL url) {
        String host = url.getParameters().get("host");
        String port = url.getParameters().get("port");
        String group = url.getParameters().get("group");
        return new String((url.getApplicationName() + ";" + url.getServiceName() + ";" + host + ":" + port + ";" + System.currentTimeMillis() + ";100;" + group).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将URL转换为写入zk的consumer节点下的一段字符串
     *
     * @param url
     * @return
     */
    public static String buildConsumerURLStr(URL url) {
        String host = url.getParameters().get("host");
        return new String((url.getApplicationName() + ";" + url.getServiceName() + ";" + host + ";" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将某个节点下的信息转换为一个ProviderNode对象
     *
     * @param providerNodeStr
     * @return
     */
    public static ProviderNodeInfo buildProviderNodeFromUrlStr(String providerNodeStr) {
        String[] items = providerNodeStr.split(";");
        ProviderNodeInfo providerNodeInfo = new ProviderNodeInfo();
        providerNodeInfo.setApplicationName(items[0]);
        providerNodeInfo.setServiceName(items[1]);
        providerNodeInfo.setAddress(items[2]);
        providerNodeInfo.setRegistryTime(items[3]);
        providerNodeInfo.setWeight(Integer.parseInt(items[4]));
        providerNodeInfo.setGroup(items[5]);
        return providerNodeInfo;
    }
}
