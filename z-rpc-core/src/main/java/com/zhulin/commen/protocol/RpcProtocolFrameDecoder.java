package com.zhulin.commen.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import static com.zhulin.commen.cache.CommonClientCache.CLIENT_CONFIG;
import static com.zhulin.commen.cache.CommonServerCache.SERVER_CONFIG;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/17
 * @Description: rpc传输消息长度规定，防止粘包和半包现象
 */
public class RpcProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {
    public RpcProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                                   int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    public RpcProtocolFrameDecoder() {
        this(SERVER_CONFIG != null ? SERVER_CONFIG.getMaxServerRequestData() :
                CLIENT_CONFIG.getMaxServerRespDataSize(), 2, 4, 0, 0);
    }
}
