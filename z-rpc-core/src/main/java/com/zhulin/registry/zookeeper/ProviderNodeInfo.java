package com.zhulin.registry.zookeeper;

import lombok.Data;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/26
 * @Description: zk的provider节点信息类
 */
@Data
public class ProviderNodeInfo {
    private String applicationName;
    private String serviceName;
    private String address;
    private String registryTime;
    private Integer weight;
    private String group;
}
