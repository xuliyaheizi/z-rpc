package com.zhulin.commen.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.CountDownLatch;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description:
 */
@Data
@AllArgsConstructor
public class PackingRpcInfoContent {
    private RpcInfoContent rpcInfoContent;
    private CountDownLatch countDownLatch;
}
