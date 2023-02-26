package com.zhulin.commen.protocol;

import com.zhulin.commen.constants.RpcConstants;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: z-rpc传输自定义协议体
 */
@Data
public class RpcProtocol implements Serializable {

    /**
     * 协议开头的魔数，主要是在做服务通讯的时候定义的一个安全检测，确认当前请求的协议是否合法。
     */
    private Short magicNum = RpcConstants.MAGIC_NUM;
    /**
     * 传输内容长度
     */
    private int contentLength;
    /**
     * 传输长度
     */
    private byte[] content;

    public RpcProtocol(byte[] content) {
        this.contentLength = content.length;
        this.content = content;
    }
}
