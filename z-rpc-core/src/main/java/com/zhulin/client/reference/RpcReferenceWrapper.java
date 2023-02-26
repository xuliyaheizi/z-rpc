package com.zhulin.client.reference;

import lombok.Data;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 代理包装类
 */
@Data
public class RpcReferenceWrapper<T> {
    /**
     * 需要代理实现的类
     */
    Class<T> aimClass;
}
