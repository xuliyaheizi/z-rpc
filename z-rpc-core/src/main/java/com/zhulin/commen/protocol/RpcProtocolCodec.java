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
     * 序列化
     *
     * @param ctx         ChannelHandlerContext
     * @param rpcProtocol RpcProtocol
     * @param list        List<Object>
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcProtocol rpcProtocol, List<Object> list) {
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
     * @param ctx     ChannelHandlerContext
     * @param byteBuf ByteBuf
     * @param list    List<Object>
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        //协议体开头标准长度
        int protocolHeaderLength = 2 + 4;
        //可读字节长度大于协议体开头标准长度
        if (byteBuf.readableBytes() >= protocolHeaderLength) {
            //判断传输协议是否合法
            if (byteBuf.readShort() != RpcConstants.MAGIC_NUM) {
                ctx.close();
                return;
            }
            //读取内容长度
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}
