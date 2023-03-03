package com.zhulin.spring.starter.common;

import java.lang.annotation.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 服务代理注解，启动客户端
 */
//注解在属性上
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZRpcReference {
    String url() default "";

    String group() default "default";

    String serviceToken() default "";

    int timeOut() default 3000;

    int retry() default 1;

    boolean async() default false;
}
