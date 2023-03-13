package com.zhulin.serializer.impl;

import com.zhulin.serializer.SerializeFactory;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/5
 * @Description:
 */
public class ProtostuffSerializeFactory implements SerializeFactory {
    /**
     * Avoid re applying buffer space every time serialization
     */
    private static final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

    @Override
    public <T> byte[] serialize(T t) {
        Class<?> aClass = t.getClass();
        Schema schema = RuntimeSchema.getSchema(aClass);
        byte[] bytes;
        try {
            bytes = ProtostuffIOUtil.toByteArray(t, schema, BUFFER);
        } finally {
            BUFFER.clear();
        }
        return bytes;
    }

    @Override
    public <T> T deSerialize(Class<T> clazz, byte... bytes) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        T t = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, t, schema);
        return t;
    }
}
