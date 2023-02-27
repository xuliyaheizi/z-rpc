package com.zhulin.serializer.impl;

import com.alibaba.fastjson.JSON;
import com.zhulin.serializer.SerializerFactory;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/19
 * @Description:
 */
public class FastJsonSerializeFactory implements SerializerFactory {
    @Override
    public <T> byte[] serializer(T t) {
        String jsonStr = JSON.toJSONString(t);
        return jsonStr.getBytes();
    }

    @Override
    public <T> T deSerializer(Class<T> clazz, byte[] bytes) {
        return JSON.parseObject(new String(bytes), clazz);
    }

}
