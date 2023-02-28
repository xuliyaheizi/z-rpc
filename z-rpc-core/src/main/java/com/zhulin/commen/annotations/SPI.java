package com.zhulin.commen.annotations;

import java.lang.annotation.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description:
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SPI {

    String value() default "";
}
