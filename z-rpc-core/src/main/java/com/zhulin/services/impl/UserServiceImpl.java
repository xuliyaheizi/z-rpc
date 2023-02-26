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
        return msg + " hello world";
    }
}
