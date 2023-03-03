package com.zhulin.consumer.controller;

import com.zhulin.interfaces.UserService;
import com.zhulin.spring.starter.common.ZRpcReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/1
 * @Description:
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @ZRpcReference(retry = 2)
    private UserService userService;

    @GetMapping("/sayHello")
    public String sayHello(@RequestParam String msg) {
        String info = userService.sayHello(msg);
        System.out.println(info);
        return info;
    }

    @GetMapping("/test")
    public String test() {
        String resp = "test";
        return resp;
    }
}
