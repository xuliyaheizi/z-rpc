package provider.serviceImpl;

import com.zhulin.interfaces.UserService;
import com.zhulin.spring.starter.common.ZRpcServer;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/1
 * @Description:
 */
@ZRpcServer(limit = 100)
public class UserServiceImpl implements UserService {
    @Override
    public String sayHello(String msg) {
        return msg + " hello world";
    }
}
