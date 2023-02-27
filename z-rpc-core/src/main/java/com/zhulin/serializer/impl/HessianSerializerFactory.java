package com.zhulin.serializer.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.zhulin.serializer.SerializerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/19
 * @Description: Hessian序列化技术
 */
public class HessianSerializerFactory implements SerializerFactory {
    @Override
    public <T> byte[] serializer(T t) {
        byte[] data = null;
        try {
            ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(byteOs);
            output.writeObject(t);
            output.getBytesOutputStream().flush();
            output.completeMessage();
            output.close();
            data = byteOs.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Hessian序列化失败", e);
        }
        return data;
    }

    @Override
    public <T> T deSerializer(Class<T> clazz, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        T result = null;
        try {
            ByteArrayInputStream byteIs = new ByteArrayInputStream(bytes);
            Hessian2Input input = new Hessian2Input(byteIs);
            result = (T) input.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

}
