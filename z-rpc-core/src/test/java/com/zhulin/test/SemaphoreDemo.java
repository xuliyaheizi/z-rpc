package com.zhulin.test;

import java.util.concurrent.Semaphore;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description:
 */
public class SemaphoreDemo {
    public static void main(String[] args) {
        Semaphore semaphore=new Semaphore(2);
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 获取资源，若此时资源被用光，则阻塞，直到有线程归还资源
                        semaphore.acquire();
                        System.out.println(Thread.currentThread().getName() + "获取资源");
                        Thread.sleep(3000);
                        semaphore.release();
                        System.out.println(Thread.currentThread().getName() + "释放资源");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        Thread.yield();
    }
}
