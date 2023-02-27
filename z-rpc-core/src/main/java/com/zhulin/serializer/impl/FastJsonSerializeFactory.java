package com.zhulin.serializer.impl;

import com.alibaba.fastjson.JSON;
import com.zhulin.serializer.SerializeFactory;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/19
 * @Description:
 */
public class FastJsonSerializeFactory implements SerializeFactory {
    @Override
    public <T> byte[] serialize(T t) {
        String jsonStr = JSON.toJSONString(t);
        return jsonStr.getBytes();
    }

    @Override
    public <T> T deSerialize(Class<T> clazz, byte... bytes) {
        return JSON.parseObject(new String(bytes), clazz);
    }

}
