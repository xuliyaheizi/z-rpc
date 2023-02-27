package com.zhulin.commen.event.data;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/17
 * @Description:
 */
@Data
public class URLChangeWrapper {
    /**
     * 服务名称
     */
    private String serviceName;
    /**
     * 提供者的Url
     */
    private List<String> providerUrl;
    /**
     * 记录每个ip下边的url详细信息，包括权重，分组等
     */
    private Map<String,String> nodeDataUrl;
}
