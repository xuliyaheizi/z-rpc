package com.zhulin.commen.channel;

import io.netty.channel.ChannelFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/26
 * @Description: 与服务端连接后的通道包装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelFutureWrapper {
    private ChannelFuture channelFuture;
    private String host;
    private Integer port;
    private Integer weight;
    private String group;
}
