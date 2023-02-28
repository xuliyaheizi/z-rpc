package com.zhulin.services.impl;

import com.zhulin.services.UserService;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description:
 */
public class UserServiceImpl implements UserService {
    @Override
    public String sayHello(String msg) {
        int i = 1 / 0;
        return msg + " hello world";
    }
}
