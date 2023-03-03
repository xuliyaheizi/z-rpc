package com.zhulin.commen.concurrent;

import lombok.Data;

import java.util.concurrent.Semaphore;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 服务限流包装类
 */
@Data
public class ServiceSemaphoreWrapper {
    private Semaphore semaphore;
    private int maxNums;

    public ServiceSemaphoreWrapper(int maxNums) {
        this.semaphore = new Semaphore(maxNums);
        this.maxNums = maxNums;
    }
}
