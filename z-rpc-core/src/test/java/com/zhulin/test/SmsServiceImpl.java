package com.zhulin.test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/16
 * @Description:
 */
public class SmsServiceImpl implements SmsService {
    @Override
    public String send(String msg) {
        return "cnm" + msg;
    }

    public static void main(String[] args) throws InterruptedException {
        List<String> test = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    for (int j = 0; j < 10; j++) {
                        test.add("abc");
                    }
                }
            }).start();
        }
        Thread.sleep(2000);
        startLatch.countDown();
        Thread.sleep(3000);
        System.out.println(test);
    }
}
