package com.zhulin.spring.starter.common;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 服务注册注解，启动服务端
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ZRpcServer {
    int limit() default 0;

    String group() default "default";

    String serviceToken() default "";
}
