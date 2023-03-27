package com.zhulin.test;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/16
 * @Description:
 */
public class SmsProxy implements SmsService {

    private final SmsService smsService;

    public SmsProxy(SmsService smsService) {
        this.smsService = smsService;
    }

    @Override
    public String send(String msg) {
        smsService.send(msg);
        return null;
    }
}
