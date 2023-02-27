package com.zhulin.router;

import com.zhulin.commen.channel.ChannelFutureWrapper;
import lombok.Data;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/27
 * @Description: 筛选器
 */
@Data
public class Selector {
    /**
     * 服务提供者的服务名
     */
    private String providerServiceName;
    /**
     * 经过二次筛选后的future数组
     */
    private ChannelFutureWrapper[] channelFutureWrappers;
}
