package com.zhulin.commen.protocol;

import com.zhulin.commen.constants.RpcConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: z-rpc传输协议解编码器
 */
@Slf4j
@ChannelHandler.Sharable
public class RpcProtocolCodec extends MessageToMessageCodec<ByteBuf, RpcProtocol> {
    /**
     * 协议体开头标准长度
     */
    private final int BASE_LENGTH = 2 + 4;

    /**
     * 序列化
     *
     * @param ctx
     * @param rpcProtocol
     * @param list
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcProtocol rpcProtocol, List<Object> list) throws Exception {
        ByteBuf buffer = ctx.alloc().buffer();
        //魔数
        buffer.writeShort(rpcProtocol.getMagicNum());
        //内容长度
        buffer.writeInt(rpcProtocol.getContentLength());
        //内容
        buffer.writeBytes(rpcProtocol.getContent());
        list.add(buffer);
    }

    /**
     * 反序列化
     *
     * @param ctx
     * @param byteBuf
     * @param list
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() >= BASE_LENGTH) {
            //判断魔数
            if (byteBuf.readShort() != RpcConstants.MAGIC_NUM) {
                ctx.close();
                return;
            }
            int length = byteBuf.readInt();
            if (byteBuf.readableBytes() < length) {
                //说明剩余的数据包不完整
                ctx.close();
                return;
            }
            byte[] data = new byte[length];
            byteBuf.readBytes(data);
            RpcProtocol rpcProtocol = new RpcProtocol(data);
            list.add(rpcProtocol);
        }
    }
}
