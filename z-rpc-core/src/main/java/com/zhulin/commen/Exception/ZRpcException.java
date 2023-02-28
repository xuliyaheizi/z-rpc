package com.zhulin.commen.Exception;

import com.zhulin.commen.protocol.RpcInfoContent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 自定义异常类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZRpcException extends RuntimeException {
    private RpcInfoContent rpcInfoContent;
}
