# Z-RPC项目介绍
## 一、模块介绍

```txt
├─client				-> 客户端
├─commen				
│  ├─annotations		-> 注解包
│  ├─cache				-> 项目缓存
│  ├─channel			-> 处理netty的channel通道
│  ├─concurrent			-> 多线程、并发类
│  ├─config				-> 加载项目配置文件
│  ├─constants			-> 常量
│  ├─event				-> 事件处理类
│  ├─Exception			-> 异常类
│  ├─protocol			-> 自定义协议体、传输信息编解码器
│  └─utils				-> 工具类
├─filter				-> 过滤链、过滤器
├─proxy					-> 代理层
├─registry				-> 注册中心
├─router				-> 路由层、负载均衡
├─serializer			-> 序列化技术
├─server				-> 服务端
└─spi					-> SPI技术、可插拔组件
```

### 1.1、配置文件

**Server配置文件**

```properties
#netty服务端口号
zrpc.serverPort=9002
#应用名
zrpc.applicationName=zrpc-UserProvider9001
#代理技术类型
zrpc.proxyType=jdk
#路由，负载均衡技术
zrpc.router=random
#序列化方式
zrpc.serverSerialize=hessian
#注册远程中心地址
zrpc.registerAddr=zhulinz.top:20011
#注册中心类型
zrpc.registerType=zookeeper
#线程池线程数量
zrpc.server.biz.thread.nums=
#线程池队列大小
zrpc.server.queue.size=
#服务端最大连接数
zrpc.server.max.connection=
#服务端最大传输数据体积
zrpc.server.max.data.size=10*1024
```

**Client配置文件**

```properties
#应用名
zrpc.applicationName=zrpc-conusmer
#代理技术类型
zrpc.proxyType=jdk
#路由、负载均衡技术
zrpc.router=random
#序列化方式
zrpc.clientSerialize=hessian
#注册远程中心地址
zrpc.registerAddr=zhulinz.top:20011
#注册中心类型
zrpc.registerType=zookeeper
#客户端响应等待时间
zrpc.client.default.timeout=3000
#客户端最大传输数据体积
zrpc.client.max.data.size=10*1024
```

### 1.1、RPC调用流程分析

一般RPC的基本调用流程是首先本地的客户端需要通知一个本地的存根(stub)，然后存根需要进行一些数据格式的包装，网络请求的封装，按照一定的规则将这个数据包发送到指定的目标机器上。

服务端的存根在接收到相应数据后，需要按照事先约好的规则进行解码，从而识别到内部数据，然后将对应的请求转发到本地服务对应的函数中进行处理。处理的数据正常返回给调用方。

调用发的存根在接收到服务方数据后，需要进行数据解码，最后得到调用结果。

<img src="https://oss.zhulinz.top/newImage202302252249174.png" alt="image-20230225224904495"  />

## 二、引入Proxy代理层

在设计远程过程调用框架时，客户端调用远程方法时理应像调用本地方法一样，让使用者更简洁的调用方法，而其中的细节就需要被封装屏蔽。这时就需要一个代理层，统一将内部的细节隐蔽起来，让调用者无感知。

**代理模式的优点**：

1. 在客户端与目标对象之间起到一个中介作用和保护目标对象的作用。
1. 扩展目标对象的功能。
1. 将客户端与目标对象分离，在一定程度上降低了系统的耦合度，增加了程序的可扩展性。

**JDK动态代理**：在程序执行过程中，创建代理对象，通过代理对象执行方法，给目标类的方法增加额外的功能，也叫做方法增强。

**实现步骤**：

1. 首先我们需要有一个目标类，在目标类的基础上通过动态代理实现功能增强
2. 创建InvocationHandler接口的实现类，在这个类中实现invoke方法，在invoke方法中实现给目标类的方法增强功能
3. 通过JDK中的Proxy创建代理，通过代理调用目标类中的方法，实现功能增强

### 2.1、JDK代理层设计

JDK代理核心逻辑，将请求信息放入发送队列`SEND_QUEUE`中，`RESP_MAP`中根据`key`为请求信息的`UUID`放入一个`TimeoutInvocation`，`TimeoutInvocation`中有一个内容空的`RpcInfoContent`和`计数为1`的`CountDownLatch`，在规定时间内客户端收到服务端的响应信息后就根据`UUID`存入`RESP_MAP`中且唤醒代理类中的线程。

```java
if (rpcReferenceWrapper.isAsync()) {
    //不需要获取响应数据，只需要向服务端发起一步请求即可
    //将传输内容添加到消息队列中
    SEND_QUEUE.add(rpcInfoContent);
    return null;
}
//将该请求保存在响应集合中
TimeoutInvocation timeoutInvocation = new TimeoutInvocation(null);
RESP_MAP.put(rpcInfoContent.getUuid(), timeoutInvocation);
//将传输内容添加到消息队列中
SEND_QUEUE.add(rpcInfoContent);
//判断是否超时 或者 是否设置了重试机制
Boolean isNotTimeOut = timeoutInvocation.tryAcquire(timeOut, TimeUnit.MILLISECONDS);
if (isNotTimeOut || rpcInfoContent.getRetry() > 0) {
    RpcInfoContent respInfo = ((TimeoutInvocation) RESP_MAP.remove(rpcInfoContent.getUuid())).getRpcInfoContent();
    if (isNotTimeOut && respInfo.getE() == null) {
        //为超时且没有异常信息
        return respInfo.getResponse();
    } else if (respInfo.getRetry() > 0) {
        //重试次数++
        retryTimes++;
        //重试，重新设置UUID
        //rpcInfoContent.setUuid(UUID.randomUUID().toString());
        respInfo.setResponse(null);
        respInfo.setE(null);
        respInfo.setRetry(respInfo.getRetry() - 1);
        //回调接收重试之后的响应数据
        return tryFinishedTask(respInfo, rpcReferenceWrapper);
    } else if (respInfo.getE() != null) {
        respInfo.getE().printStackTrace();
    }
}
RESP_MAP.remove(rpcInfoContent.getUuid());
throw new TimeoutException("Wait for response from server on client " + timeOut + "ms，retry times is " + retryTimes + ",service's name is " + rpcInfoContent.getTargetServiceName() + "#" + rpcInfoContent.getTargetMethod());
```

```java
@Data
public class TimeoutInvocation {
    private final CountDownLatch countDownLatch;
    private RpcInfoContent rpcInfoContent;

    public TimeoutInvocation(RpcInfoContent rpcInfoContent) {
        this.countDownLatch = new CountDownLatch(1);
        this.rpcInfoContent = rpcInfoContent;
    }

    public Boolean tryAcquire(long timeOut, TimeUnit timeUnit) throws InterruptedException {
        return countDownLatch.await(timeOut, timeUnit);
    }

    public void release() {
        countDownLatch.countDown();
    }
}
```

#### 基本流程

1. Client启动时会启动一个异步线程阻塞队列，等待接收代理类放入的`RpcInfoContent`，并将其按照顺序发送给`Server`。

   ```java
   @Override
   public void run() {
       while (true) {
           try {
               //从消息队列中获取信息内容
               RpcInfoContent rpcInfoContent = SEND_QUEUE.take();
               //发送消息
               ChannelFuture channelFuture = ConnectionHandler.getChannelFuture(rpcInfoContent);
               if (channelFuture != null) {
                   Channel channel = channelFuture.channel();
                   //如果出现服务端中断的情况需要兼容下
                   if (!channel.isOpen()) {
                       throw new RuntimeException("aim channel is not open!rpcInfoContent is " + rpcInfoContent);
                   }
                   //通过序列化方式将信息序列化为字节数组
                   RpcProtocol rpcProtocol = new RpcProtocol(CLIENT_SERIALIZE_FACTORY.serialize(rpcInfoContent));
                   channel.writeAndFlush(rpcProtocol);
               }
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
   }
   ```

2. Client首先通过一个代理工厂获取被调用对象的代理对象，然后通过代理对象设置`RpcInfoContent`必要的参数，并将此放入发送队列

    - 如：目标方法，目标服务，请求参数，`UUID`，其中UUID是为了保证Client接收结果时数据一致

3. 异步线程阻塞队列阻塞式的获取到`RpcInfoContent`后，将其封装为`RpcProtocol`（自定义传输协议体），经过`EnCode编码`后，发送给`Server`。将请求发送任务交给**单独的IO线程区**负责，实现异步化，提升发送性能。

4. Server收到信息后通过`DeCode解码`，获取到`RpcProtocol`，得到其中的`content`，并转为`RpcInfoContent`类。从该类中获取对应的目标服务属性，通过目标服务属性从`PROVIDER_MAP`中获取对应的服务实现类（`PROVIDER_MAP`在Server启动时就已将需要暴露的服务注册进其中），最后在目标服务中找到对应方法并执行得到响应结果。

5. 将响应结果塞入RpcInfoContent，再次封装为RpcProtocol通过EnCode发送给客户端。

   ```java
   //获取客户端请求的协议体
   RpcProtocol rpcProtocol = channelReadData.getRpcProtocol();
   //反序列化得到请求内容
   RpcInfoContent rpcInfoContent = SERVER_SERIALIZE_FACTORY.deSerialize(RpcInfoContent.class, rpcProtocol.getContent());
   //执行过滤链路
   try {
       //执行前置过滤器
       SERVER_BEFORE_FILTER_CHAIN.doFilter(rpcInfoContent);
   } catch (Exception cause) {
       //捕捉异常信息
       if (cause instanceof ZRpcException) {
           ZRpcException zRpcException = (ZRpcException) cause;
           rpcInfoContent.setE(zRpcException);
           RpcProtocol respProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInfoContent));
           channelReadData.getCtx().writeAndFlush(respProtocol);
           return;
       }
   }
   //在服务端暴露的提供服务集合中通过服务名获取服务
   Object aimClass = PROVIDER_MAP.get(rpcInfoContent.getTargetServiceName());
   //获取该服务的方法
   Method[] methods = aimClass.getClass().getDeclaredMethods();
   Object result = null;
   //遍历方法，反射执行目标方法
   for (Method method : methods) {
       if (method.getName().equals(rpcInfoContent.getTargetMethod())) {
           if (method.getReturnType().equals(Void.TYPE)) {
               try {
                   method.invoke(aimClass, rpcInfoContent.getArgs());
               } catch (Exception e) {
                   rpcInfoContent.setE(e);
               }
           } else {
               try {
                   result = method.invoke(aimClass, rpcInfoContent.getArgs());
               } catch (Exception e) {
                   e.printStackTrace();
                   rpcInfoContent.setE(e);
               }
           }
           //跳出循环
           break;
       }
   }
   //写入响应数据
   rpcInfoContent.setResponse(result);
   //执行后置过滤器
   SERVER_AFTER_FILTER_CHAIN.doFilter(rpcInfoContent);
   RpcProtocol respProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInfoContent));
   //给客户端发送响应数据
   channelReadData.getCtx().writeAndFlush(respProtocol);
   ```

6. `Client`接收到响应数据后，通过`Decode`解码转为`RpcProtocol`，获取到`RpcInfoContent`。根据之前的`RESP_MAP`集合判断请求与响应的`UUID`是否一致，一致则将`RpcInfoContent`塞入`TimeoutInvocation`，再通过`TimeoutInvocation`中的`CountDownLatch`的`countDown`方法唤醒代理中的等待线程，然后获取到`RpcInfoContent`的响应结果，并返回给`Client`。

   ```java
   #ClientReadHandler
   //将请求的响应结构放入一个Map集合中，集合的key就是uuid，这个uuid在发送请求之前就已经初始化好了，所以只需要起一个线程在后台遍历这个map，查看对应的key是否有相应即可。
   TimeoutInvocation timeoutInvocation = (TimeoutInvocation) RESP_MAP.get(rpcInfoContent.getUuid());
   timeoutInvocation.setRpcInfoContent(rpcInfoContent);
   RESP_MAP.put(rpcInfoContent.getUuid(), timeoutInvocation);
   timeoutInvocation.release();
   ```

   ```java
   #JDKInvocationHandler.tryFinishedTask()
   //判断是否超时
   if (timeoutInvocation.tryAcquire(timeOut, TimeUnit.MILLISECONDS)) {
       return ((TimeoutInvocation) RESP_MAP.remove(rpcInfoContent.getUuid())).getRpcInfoContent().getResponse();
   }
   ```

## 三、引入zookeeper注册中心

问题：

- 假如一个服务有10台不同的机器进行提供，那客户端应该如何去获取这10台目标机器的ip地址信息？
- 随着调用方的增加，如何对服务调用者的数据进行监控？
- 服务提供者下线了，然后通知到服务调用方？
- 此时就需要一个第三方平台，每个服务暴露的时候，将相关信息记录到这个中间平台。当有调用方订阅服务的时候，也需要预先到中间平台上进行登记。当服务提供者下线的时候，需要到该平台上去将之前的记录移除，然后通知相应的服务调用方。


​	<img src="https://oss.zhulinz.top/newImage202302261448307.png" alt="image-20230226144813372" width="60%" />

### 3.1、Zookeeper

- zookeeper和客户端之间可以构成主动推送，能够实现服务上线和下线时的通知效果。
- Zookeeper自身提供了高可用的机制，并且对于数据节点的存储可以支持顺序、非顺序、临时、持久化的特性。

### 3.2、注册节点的结构化设计

先定义一个RPC的根节点`zrpc`，接着是不同的服务名称(com.zhulin.service.UserService)作为二级节点，在二级节点下划分consumer和provider节点。consumer节点存放具体的服务调用名和地址，provider节点存放的数据以ip+端口的格式存储。

<img src="https://oss.zhulinz.top/newImage202302261501030.png" alt="image-20230226150138969" width="60%" />

```
/ZRPC/com.zhulin.services.UserService/provider/192.168.100.141:8080
节点数据：zrpc-test;com.zhulin.services.UserService;192.168.100.141:8080;1677413355492;100
```

### 3.3、Server端实现

```java
#RpcServer main()
RpcServer rpcServer = new RpcServer();
//初始化基本服务
rpcServer.initServerConfig();
//事件监听机制
IRpcListenerLoader iRpcListenerLoader = new IRpcListenerLoader();
iRpcListenerLoader.init();
//向注册中心注册所暴露的服务接口
rpcServer.registryService(new UserServiceImpl());
rpcServer.startApplication();
//监听服务注销线程
ApplicationShutDownHook.registryShutdownHook();
```

- 在`registryService()`方法中，将所暴露的**服务实现类**添加到`Map`中，**服务提供者信息**添加到`Set`中

  URL类是配置类，基于其进行存储

  ```java
  //需要注册的对象统一放在一个MAP集合中进行管理
  PROVIDER_MAP.put(interfaceClass.getName(), serviceBean);
  //构建服务注册信息
  URL url = new URL();
  url.setApplicationName(SERVER_CONFIG.getApplicationName());
  url.setServiceName(interfaceClass.getName());
  url.addParameter("host", CommonUtil.getIpAddress());
  url.addParameter("port", String.valueOf(SERVER_CONFIG.getServerPort()));
  PROVIDER_URL_SET.add(url);
  ```

- 在`startApplication()`方法中，启动Netty服务端并调用`batchRegistryUrl()`方法，开启异步任务，从`PROVIDER_URL_SET`中获取`URL`，进行服务注册。

  ```java
  for (URL url : PROVIDER_URL_SET) {
      REGISTRY_SERVICE.register(url);
      log.info("[Server] export service {}", url.getServiceName());
  }
  ```

- 最后启动`ApplicationShutDownHook.registryShutdownHook()`监听服务注销线程。

  ```java
  #ApplicationShutDownHook registryShutdownHook()
  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
          log.info("[registryShutdownHook] ======= Server Destroy ======");
          ZRpcListenerLoader.sendSyncEvent(new ZRpcDestroyEvent("destroy"));
      }
  }, "serverDestroyTask"));
  ```

  然后通过事件监听加载器去执行注销的事件方法

  ```java
  #ServiceDestroyListener
  for (URL url : PROVIDER_URL_SET) {
      //注销服务
      REGISTRY_SERVICE.unRegister(url);
  }
  ```

### 3.4、Client端实现

```java
# main()
RpcClient rpcClient = new RpcClient();
//设置代理包装类
RpcReferenceWrapper rpcReferenceWrapper = new RpcReferenceWrapper();
rpcReferenceWrapper.setAimClass(UserService.class);
RpcReference reference = rpcClient.initApplication();
//订阅服务
rpcClient.doSubscribeService(UserService.class);
//连接服务
ConnectionHandler.bootstrap = rpcClient.getBootstrap();
//与服务提供者建立连接
rpcClient.doConnectServer();
//异步进行网络通信
rpcClient.startSendMsg();
UserService userService = (UserService) reference.get(rpcReferenceWrapper);
System.out.println(userService.sayHello("zhulin"));
```

- 在`initApplication()`方法中，启动Netty客户端，并进行事件监听器的初始化

  在`init()`方法中，向`zRpcListenerLoader`中添加了`ServiceUpdateListener`监听器

  ```java
  //初始化事件监听器
  zRpcListenerLoader = new ZRpcListenerLoader();
  zRpcListenerLoader.init();
  ```

- 在`doSubscribeService()`方法中，初始化注册中心`REGISTRY_SERVICE`，并定义`URL`向注册中心发起订阅。

  ```java
  //构建订阅信息
  URL url = new URL();
  url.setApplicationName(CLIENT_CONFIG.getApplicationName());
  url.setServiceName(serviceBean.getName());
  url.addParameter("host", CommonUtil.getIpAddress());
  //获取注册中心的该服务的权重信息
  Map<String, String> result = REGISTRY_SERVICE.getProviderNodeInfos(serviceBean.getName());
  URL_MAP.put(serviceBean.getName(), result);
  //向注册中心发起订阅
  REGISTRY_SERVICE.subscriber(url);
  ```

- 在`doConnectServer()`方法中，提前与所有已注册的服务建立连接，并监听这些服务的变化（上线、下线、更改等）

    - `SUBSCRIBER_SERVICE_LIST`集合为订阅时添加的URL集合

  ```java
  for (URL providerUrl : SUBSCRIBER_SERVICE_LIST) {
      List<String> providerIps = REGISTRY_SERVICE.getProviderIps(providerUrl.getServiceName());
      for (String providerIp : providerIps) {
          try {
              ConnectionHandler.connect(providerUrl.getServiceName(), providerIp);
          } catch (InterruptedException e) {
              log.error("[doConnectServer] connect fail ", e);
          }
      }
      URL url = new URL();
      //servicePath ---> com.zhulin.services.UserService/provider
      url.addParameter("servicePath", providerUrl.getServiceName() + "/provider");
      url.addParameter("providerIps", JSON.toJSONString(providerIps));
      //客户端在此新增一个订阅功能
      REGISTRY_SERVICE.doAfterSubscribe(url);
  }
  ```

### 3.5、监听事件机制实现

订阅服务之后开启监听事件，主要用于监听已注册服务的变化。

**Zookeeper监听器原理**

1. 首先要有一个`main()`线程。
2. `main()`线程中创建**Zookeeper客户端**，此时会创建两个线程`connect`和`listen`。`connect`线程负责网络连接通信，`listen`线程负责监听。
3. 通过`connect`线程将中注册的监听事件发送给**Zookeeper服务端**。
4. 在Zookeeper的注册监听列表中将注册的监听事件添加到列表中，表示这个服务器的`/path`被客户端监听了。
5. 一旦被监听的服务器根目录下，数据或路径发生变化，Zookeeper服务端就会将这个消息发送给listen线程。
6. listen线程内部调用process()方法，执行相应的措施。

<img src="https://oss.zhulinz.top/newImage202303030009678.png" alt="image-20230303000912624" width="60%" />

**Watch监听**

Watcher实现由三个部分组成，分别是Zookeeper服务端、Zookeeper客户端以及客户端的ZKWatchManager对象，客户端首先将 Watcher注册到服务端，同时将 Watcher对象保存到客户端的watch管理器中。当Zookeeper服务端监听的数据状态发生变化时，服务端会主动通知客户端，接着客户端的 Watch管理器会触发相关 Watcher来回调相应处理逻辑，从而完成整体的数据发布/订阅流程。

<table>
    <tr>
    	<th>KeeperState</th>
        <th>EventType</th>
        <th>触发条件</th>
        <th>说明</th>
    </tr>
    <tr>
    	<td rowspan=5>SyncConnected</td>
        <td>None</td>
        <td>客户端与服务端成功建立连接</td>
        <td rowspan=5>客户端和服务器处于连接状态</td>
    </tr>
    <tr>
    	<td>NodeCreated</td>
        <td>Watcher监听的对应数据节点被创建</td>
    </tr>
    <tr>
    	<td>NodeDeleted</td>
        <td>Watcher监听的对应数据节点被删除</td>
    </tr>
    <tr>
    	<td>NodeDataChanged</td>
        <td>Watcher监听的对应数据节点的数据内容发生变更</td>
    </tr>
    <tr>
    	<td>NodeChildChanged</td>
        <td>Wather监听的对应数据节点的子节点列表发生变更</td>
    </tr>
    <tr>
    	<td>Disconnected</td>
        <td>None</td>
        <td>客户端与ZooKeeper服务器断开连接</td>
        <td>客户端与服务器断开连接时</td>
    </tr>
    <tr>
    	<td>Expired</td>
        <td>None</td>
        <td>会话超时</td>
        <td>会话session失效时</td>
    </tr>
    <tr>
    	<td>AuthFailed</td>
        <td>None</td>
        <td>通常有两种情况，1：使用错误的schema进行权限检查 2：SASL权限检查失败</td>
        <td>身份认证失败时</td>
    </tr>
</table>


Watcher监听器是一次性的。利用Watcher来对节点进行监听操作，当事件被触发之后，所对应的 watcher 会被立马删除，如果要反复使用，就需要反复的使用usingWatcher提前注册。所以，Watcher监听器不能应用于节点的数据变动或者节点变动这样的一般业务场景。而是适用于一些特殊的，比如会话超时、授权失败等这样的特殊场景。

**Cache监听**

Curator引入了Cache来监听ZooKeeper服务端的事件。Cache事件监听可以理解为一个本地缓存视图与远程Zookeeper视图的对比过程，简单来说，Cache在客户端缓存了znode的各种状态，当感知到zk集群的znode状态变化，会触发event事件，注册的监听器会处理这些事件。Cache对ZooKeeper事件监听进行了封装，能够自动处理反复注册监听，主要有以下三类：

| 类名              | 用途                                                         |
| ----------------- | ------------------------------------------------------------ |
| NodeCache         | 监听节点对应增、删、改操作                                   |
| PathChildrenCache | 监听节点下一级子节点的增、删、改操作                         |
| TreeCache         | 可以将指定的路径节点作为根节点，对其所有的子节点操作进行监听，呈现树形目录的监听 |

**代码实现**

1. **ZRpcListenerLoader**：用于注册和管理监听器，调用相应的监听器回调方法。

   ZRpcEvent为发生事件接口，ZRpcListener为事件监听器接口

   ```java
   private static final List<ZRpcListener> zRpcListenerList = new ArrayList<>();
   //线程池
   private static final ExecutorService eventThreadPool = Executors.newFixedThreadPool(2);
   ```

   ```
   ├── registerListener(IRpcListener iRpcListener) // 注册监听器事件
   ├── sendEvent(IRpcEvent iRpcEvent) // 调用监听器对应回调方法
   ```

   sendEvent()方法实现

   ```java
   /**
    * 异步事件处理
    *
    * @param zRpcEvent
    */
   public static void sendEvent(ZRpcEvent zRpcEvent) {
       if (zRpcListenerList.isEmpty()) {
           return;
       }
       for (ZRpcListener<?> iRpcListener : zRpcListenerList) {
           // 获取listener上监听事件的泛型
           Class<?> type = getInterfaceT(iRpcListener);
           // 判断Listener监听事件的泛型是否与Watcher传递的一致
           if (type.equals(zRpcEvent.getClass())) {
               eventThreadPool.execute(new Runnable() {
                   @Override
                   public void run() {
                       try {
                           //一致则异步执行回调函数
                           iRpcListener.callBack(zRpcEvent.getData());
                       } catch (Exception e) {
                           e.printStackTrace();
                       }
                   }
               });
           }
       }
   }
   ```

2. **Zookeeper订阅后的监听逻辑**

   根据URL中的参数去执行相应的监听逻辑

   ```java
   @Override
       public void doAfterSubscribe(URL url) {
           //监听服务上线下线
           String servicePath = url.getParameters().get("servicePath");
           if (!CommonUtil.isEmpty(servicePath)) {
               // watchChildNodePath ---> /ZRPC/com.zhulin.services.UserService/provider
               String watchChildNodePath = ROOT + "/" + servicePath;
               watchChildNodeData(watchChildNodePath);
           }
           //监听服务节点数据变化
           String providerIpStrJson = url.getParameters().get("providerIps");
           if (!CommonUtil.isEmpty(providerIpStrJson)) {
               List<String> providerIpList = JSON.parseObject(providerIpStrJson, List.class);
               for (String providerIp : providerIpList) {
                   //监听服务节点信息变化
                   watchNodeDataChange(ROOT + "/" + servicePath + "/" + providerIp);
               }
           }
           //监听单个服务节点数据变化
           String providerPath = url.getParameters().get("providerPath");
           if (!CommonUtil.isEmpty(providerPath)) {
               watchNodeDataChange(ROOT + "/" + providerPath);
           }
       }
   ```

   **watchChildNodeData**：主要监听节点下的列表变化情况

   在事件`NodeChildrenChanged`的情况下去执行逻辑，`NodeChildrenChanged`说明该节点下有新服务提供者上线或者旧服务提供者下线，然后执行对应的更新事件。

   ```java
   private void watchChildNodeData(String watchChildNodePath) {
       zkClient.watchChildNodeData(watchChildNodePath, new Watcher() {
           @Override
           public void process(WatchedEvent event) {
               if (event.getType() == Event.EventType.NodeChildrenChanged) {
                   String path = event.getPath();
                   //如果childrenData为空,说明该服务已经没有提供者了
                   List<String> childrenData = zkClient.getChildrenData(path);
                   URLChangeWrapper urlChangeWrapper = new URLChangeWrapper();
                   urlChangeWrapper.setProviderUrl(childrenData);
                   urlChangeWrapper.setServiceName(path.split("/")[2]);
                   if (!CommonUtil.isEmptyList(childrenData)) {
                       Map<String, String> result = new HashMap<>();
                       for (String ipAndHost : childrenData) {
                           String childData = zkClient.getNodeData(path + "/" + ipAndHost);
                           result.put(ipAndHost, childData);
                       }
                       urlChangeWrapper.setNodeDataUrl(result);
                   }
                   //自定义的一套监听组件
                   ZRpcEvent iRpcEvent = new ZRpcUpdateEvent(urlChangeWrapper);
                   ZRpcListenerLoader.sendEvent(iRpcEvent);
                   //收到回调后再注册一次监听，这样能保证一直都收到信息
                   watchChildNodeData(path);
               }
           }
       });
   }
   ```

   监听服务提供者上线或下线的处理逻辑代码。

   **主要逻辑是**：判断此时服务下是否还有提供者，若没有则清除客户端中的相关缓存信息。若有则将现有的服务提供者去与之前连接的服务提供者比较，将已下线的服务提供者移除，与新上线的服务提供者建立连接并监听节点数据变化。

   ```java
   @Override
   public void callBack(Object t) {
       //获取字节点的数据信息
       URLChangeWrapper urlChangeWrapper = (URLChangeWrapper) t;
       if (CommonUtil.isEmptyList(urlChangeWrapper.getProviderUrl())) {
           //如果为空,说明该服务下已经没有服务提供者了
           CONNECT_MAP.remove(urlChangeWrapper.getServiceName());
           SERVICE_ROUTER_MAP.remove(urlChangeWrapper.getServiceName());
           URL_MAP.remove(urlChangeWrapper.getServiceName());
       } else {
           //根据服务名获取已连接的服务
           List<ChannelFutureWrapper> oldChannelFutureWrappers = CONNECT_MAP.get(urlChangeWrapper.getServiceName());
           //获取现有的服务提供者
           List<String> matchProviderUrl = urlChangeWrapper.getProviderUrl();
           //最终的服务提供者IP
           Set<String> finalUrl = new HashSet<>();
           //最终的服务提供者连接通道
           List<ChannelFutureWrapper> finalChannelFutureWrappers = new ArrayList<>();
           //新的服务提供者连接通道
           List<ChannelFutureWrapper> newChannelFutureWrappers = new ArrayList<>();
           if (CommonUtil.isEmptyList(oldChannelFutureWrappers)) {
               //暂未连接到服务提供者
               newChannelFutureWrappers = connectWithNewProvider(urlChangeWrapper, matchProviderUrl);
           } else {
               //遍历旧的服务提供者连接通道
               for (ChannelFutureWrapper channelFutureWrapper : oldChannelFutureWrappers) {
                   String oldServerAddress = channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort();
                   //如果老的url没有了，说明已经被移除
                   if (matchProviderUrl.contains(oldServerAddress)) {
                       finalChannelFutureWrappers.add(channelFutureWrapper);
                       finalUrl.add(oldServerAddress);
                   }
               }
               List<String> newProviderUrl = new ArrayList<>();
               for (String providerUrl : matchProviderUrl) {
                   //判断是否是新的服务提供者
                   if (!finalUrl.contains(providerUrl)) {
                       newProviderUrl.add(providerUrl);
                   }
               }
               //此时老的url已经被移除了，开始检查是否有新的url
               newChannelFutureWrappers = connectWithNewProvider(urlChangeWrapper, newProviderUrl);
           }
           finalChannelFutureWrappers.addAll(newChannelFutureWrappers);
           //最终更新服务
           CONNECT_MAP.put(urlChangeWrapper.getServiceName(), finalChannelFutureWrappers);
           Selector selector = new Selector();
           selector.setProviderServiceName(urlChangeWrapper.getServiceName());
           ZROUTER.refreshRouterArr(selector);
       }
   }
   
   /**
    * 与新上线的服务提供者建立连接
    *
    * @param urlChangeWrapper
    * @param newProviderUrl
    * @return
    */
   private List<ChannelFutureWrapper> connectWithNewProvider(URLChangeWrapper urlChangeWrapper,
                                                             List<String> newProviderUrl) {
       List<ChannelFutureWrapper> newChannelFutureWrappers = new ArrayList<>();
       for (String providerUrl : newProviderUrl) {
           //不存在，则需要添加新的url
           String host = providerUrl.split(":")[0];
           Integer port = Integer.valueOf(providerUrl.split(":")[1]);
           String urlStr = urlChangeWrapper.getNodeDataUrl().get(providerUrl);
           ProviderNodeInfo providerNodeInfo = URL.buildProviderNodeFromUrlStr(urlStr);
           ChannelFuture channelFuture = null;
           try {
               //与新的服务提供者建立连接通道
               channelFuture = ConnectionHandler.createChannelFuture(host, port);
               log.debug("channel reconnect,server is {}:{}", host, port);
               ChannelFutureWrapper channelFutureWrapper = new ChannelFutureWrapper(channelFuture, host, port,
                       providerNodeInfo.getWeight(), providerNodeInfo.getGroup());
               newChannelFutureWrappers.add(channelFutureWrapper);
               //监听节点
               URL watchUrl = new URL();
               watchUrl.addParameter("providerPath", providerNodeInfo.getServiceName() + "/provider/" + providerUrl);
               REGISTRY_SERVICE.doAfterSubscribe(watchUrl);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
       return newChannelFutureWrappers;
   }
   ```

   **watchNodeDataChange**：监听节点的数据变化

   在监听事件类型`NodeDataChanged`的情况下去执行对应的事件逻辑。

   ```java
   public void watchNodeDataChange(String watchNodeDataPath) {
       zkClient.watchNodeData(watchNodeDataPath, new Watcher() {
           @Override
           public void process(WatchedEvent watchedEvent) {
               //监听节点数据修改事件
               if (watchedEvent.getType() == Event.EventType.NodeDataChanged) {
                   String path = watchedEvent.getPath();
                   String nodeData = zkClient.getNodeData(path);
                   //nodeData = nodeData.replace(";", "/");
                   ProviderNodeInfo providerNodeInfo = URL.buildProviderNodeFromUrlStr(nodeData);
                   ZRpcEvent iRpcEvent = new ZRpcNodeChangeEvent(providerNodeInfo);
                   ZRpcListenerLoader.sendEvent(iRpcEvent);
                   watchNodeDataChange(watchNodeDataPath);
               }
           }
       });
   }
   ```

### 3.6、ConnectionHandler

按照单一职责的设计原则，将与连接有关的功能都统一封装在一起。主要用于Netty在客户端与服务端之间建立连接、断开连接、按照服务名获取连接等操作。

#### 1、Connect建立连接

```java
//获取地址和端口号
String[] ipItems = providerIp.split(":");
String host = ipItems[0];
Integer port = Integer.parseInt(ipItems[1]);
//与服务端连接
ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
//获取注册中心该服务的节点信息
String providerURLInfo = URL_MAP.get(providerServiceName).get(providerIp);
ProviderNodeInfo providerNodeInfo = URL.buildProviderNodeFromUrlStr(providerURLInfo);
//实例channelFuture通道包装类
ChannelFutureWrapper channelFutureWrapper = new ChannelFutureWrapper(channelFuture, host, port,
        providerNodeInfo.getWeight(), providerNodeInfo.getGroup());
//服务连接之后，将服务提供者的ip添加到缓存中
SERVICE_ADDRESS.add(providerIp);
//在CONNECT_MAP中获取服务提供者的信息
List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
if (CommonUtil.isEmptyList(channelFutureWrappers)) {
    channelFutureWrappers = new ArrayList<>();
}
channelFutureWrappers.add(channelFutureWrapper);
//例如com.zhulin.test.UserService会被放入到一个Map集合中，key是服务的名字，value是对应的channel通道的List集合
CONNECT_MAP.put(providerServiceName, channelFutureWrappers);
//设置路由
Selector selector = new Selector();
selector.setProviderServiceName(providerServiceName);
ZROUTER.refreshRouterArr(selector);
```

#### 2、getChannelFuture获取连接通道

每个服务可以有多个服务提供者（对应多个物理机器）

```java
String providerServiceName = rpcInfoContent.getTargetServiceName();
ChannelFutureWrapper[] channelFutureWrappers = SERVICE_ROUTER_MAP.get(providerServiceName);
if (channelFutureWrappers == null || channelFutureWrappers.length == 0) {
    rpcInfoContent.setRetry(0);
    rpcInfoContent.setE(new RuntimeException("no provider exist for " + providerServiceName));
    rpcInfoContent.setResponse(null);
    //直接交给响应线程那边处理（响应线程在代理类内部的invoke函数中，那边会取出对应的uuid的值，然后判断）
    TimeoutInvocation timeoutInvocation = (TimeoutInvocation) RESP_MAP.get(rpcInfoContent.getUuid());
    timeoutInvocation.setRpcInfoContent(rpcInfoContent);
    RESP_MAP.put(rpcInfoContent.getUuid(), timeoutInvocation);
    //通知代理类中的响应线程
    timeoutInvocation.release();
    log.error("channelFutureWrappers is null");
    return null;
}
//执行过滤器逻辑
List<ChannelFutureWrapper> channelFutureWrapperList = new ArrayList<>(channelFutureWrappers.length);
for (int i = 0; i < channelFutureWrappers.length; i++) {
	channelFutureWrapperList.add(channelFutureWrappers[i]);
}
CLIENT_FILTER_CHAIN.doFilter(channelFutureWrapperList, rpcInfoContent);
//通过负载均衡算法获取合适的服务提供者
Selector selector = new Selector();
selector.setProviderServiceName(providerServiceName);
selector.setChannelFutureWrappers(channelFutureWrappers);
return ZROUTER.select(selector).getChannelFuture();
```

## 四、引入路由层

在分布式环境中，一个服务一般有多个服务提供者，服务冗余防止单个服务提供者突入宕机而导致服务不可用。在多个服务提供者的情况下，就需要一套合适的负载均衡算法去计算合适的服务提供方。

<img src="https://oss.zhulinz.top/newImage202302271556224.png" alt="image-20230227155642113" width="50%" />

之前的负载均衡策略是简单的随机策略，通过一个random函数来随机获取。

```java
String providerServiceName = rpcInfoContent.getTargetServiceName();
List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
if (CommonUtil.isEmptyList(channelFutureWrappers)) {
    throw new RuntimeException("no provider exist for " + providerServiceName);
}
ChannelFuture channelFuture = channelFutureWrappers.get(new Random().nextInt(channelFutureWrappers.size())).getChannelFuture();
return channelFuture;
```

- 从注册中心获取服务的地址信息，并且缓存在一个MAP集合中。
- 从缓存的MAP集合中根据服务名称查询到对应的通道List集合。
- 从List集合中随机筛选一个Channel通道，发送数据包。

### 路由层实现

抽象一个路由层接口

```java
public interface ZRouter {

    /**
     * 刷新路由数组
     *
     * @param selector
     */
    void refreshRouterArr(Selector selector);

    /**
     * 获取到请求的连接通道
     *
     * @param selector
     * @return
     */
    ChannelFutureWrapper select(Selector selector);

    /**
     * 更新权重
     * @param url
     */
    void updateWeight(URL url);
}
```

#### 1、带权重的随机选取策略

自定义随机选取逻辑，将转化后的数组添加进`SERVICE_ROUTER_MAP`，权重约定为100的倍数，权重越大，服务被选取的次数也越多。

```java
//获取服务提供者的数目
List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(selector.getProviderServiceName());
ChannelFutureWrapper[] channelFutureWrapperArr = new ChannelFutureWrapper[channelFutureWrappers.size()];
//提前生成调用先后顺序的随机数组
int[] result = createRandomIndex(channelFutureWrapperArr.length);
//生成对应服务集群的每台机器的调用顺序
for (int i = 0; i < channelFutureWrappers.size(); i++) {
    channelFutureWrapperArr[i] = channelFutureWrappers.get(i);
}
SERVICE_ROUTER_MAP.put(selector.getProviderServiceName(), channelFutureWrapperArr);
//更新服务节点权重信息
URL url = new URL();
url.setServiceName(selector.getProviderServiceName());
ZROUTER.updateWeight(url);
```

#### 2、轮询策略

直接按照添加的先后顺序获取连接，将转化后的连接数组存入 SERVICE_ROUTER_MAP 中

```java
List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(selector.getProviderServiceName());
ChannelFutureWrapper[] arr = new ChannelFutureWrapper[channelFutureWrappers.size()];
for (int i = 0; i < channelFutureWrappers.size(); i++) {
    arr[i] = channelFutureWrappers.get(i);
}
SERVICE_ROUTER_MAP.put(selector.getProviderServiceName(), arr);
```

### 获取channelFuture实现

`ChannelFuturePollingRef`为实现类，用于从`SERVICE_ROUTER_MAP`中根据服务提供者名字轮询获取连接

本质是通过原子类取模运算获取连接

```java
private Map<String, AtomicLong> referenceMap = new ConcurrentHashMap<>();

public ChannelFutureWrapper getChannelFutureWrapper(Selector selector) {
    AtomicLong referCount = referenceMap.get(selector.getProviderServiceName());
    if (referCount == null) {
        referCount = new AtomicLong(0);
        referenceMap.put(selector.getProviderServiceName(), referCount);
    }
    ChannelFutureWrapper[] arr = selector.getChannelFutureWrappers();
    long i = referCount.getAndIncrement();
    //通过取模计算进行轮训
    int index = (int) (i % arr.length);
    return arr[index];
}
```

## 五、整合序列化方式

引入多种序列化策略，由用户自行配置与选择对应的策略

- FastJson
- Hessian
- Kryo
- JDK自带的序列化

### 5.1、序列化工厂

创建序列化工厂接口，定义接口方法：serialize与deSerialize（均为范型方法）

具体的序列化策略去实现该工厂类。

- SerializeFactory

- FastJsonSerializeFactory
- HessianSerializeFactory
- KryoSerializeFactory
- JdkSerializeFactory

## 六、可插拔式组件

### 6.1、SPI优势

使用Java SPI机制的优势是实现解耦，使得第三方服务模块的装配控制的逻辑与调用者的业务代码分离，而不是耦合在一起。应用程序可以根据实际业务情况启用框架扩展或替换框架组件。

相比使用提供接口jar包，供第三方服务模块实现接口的方式，SPI的方式使得源框架，不必关心接口的实现类的路径，可以不用通过下面的方式获取接口实现类：

- 代码硬编码import 导入实现类
- 指定类全路径反射获取：例如在JDBC4.0之前，JDBC中获取数据库驱动类需要通过**Class.forName("com.mysql.jdbc.Driver")**，类似语句先动态加载数据库相关的驱动，然后再进行获取连接等的操作
- 第三方服务模块把接口实现类实例注册到指定地方，源框架从该处访问实例

通过SPI的方式，第三方服务模块实现接口后，在第三方的项目代码的META-INF/services目录下的配置文件指定实现类的全路径名，源码框架即可找到实现类

### 6.2、SPI实现思路

设计一个SPI加载类，通过当前Class的类加载器去加载META-INF/irpc/目录底下存在的资源文件，在需要加载资源时（初始化序列化框架、初始化过滤链、初始化路由策略、初始化注册中心），使用SPI加载类去实现

从而避免了在代码中通过switch语句以硬编码的方式选择资源

```java
public void loadExtension(Class clazz) {
    if (clazz == null) {
        throw new IllegalArgumentException("class is null");
    }
    try {
        String spiFilePath = EXTENSION_LOADER_DIR_PREFIX + clazz.getName();
        ClassLoader classLoader = this.getClass().getClassLoader();
        Enumeration<URL> enumeration = null;
        enumeration = classLoader.getResources(spiFilePath);
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            InputStreamReader inputStreamReader = null;
            inputStreamReader = new InputStreamReader(url.openStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            LinkedHashMap<String, Class> classMap = new LinkedHashMap<>();
            //按行读取配置文件
            while ((line = bufferedReader.readLine()) != null) {
                //如果配置中加入了#开头则表示忽略该类无需进行加载
                if (line.startsWith("#")) {
                    continue;
                }
                String[] lineArr = line.split("=");
                String implClassName = lineArr[0];
                String interfaceName = lineArr[1];
                classMap.put(implClassName, Class.forName(interfaceName));
            }
            //只会触发class文件的加载，而不会触发对象的实例化
            if (EXTENSION_LOADER_CLASS_CACHE.containsKey(clazz.getName())) {
                //支持开发者自定义配置
                EXTENSION_LOADER_CLASS_CACHE.get(clazz.getName()).putAll(classMap);
            } else {
                EXTENSION_LOADER_CLASS_CACHE.put(clazz.getName(), classMap);
            }
        }
    } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
    }
}
```

**具体的配置文件**

![image-20230302225314912](https://oss.zhulinz.top/newImage202303022253039.png)

![image-20230302225407802](https://oss.zhulinz.top/newImage202303022254879.png)

## 七、责任链模式在RPC中的实现

目前框架的整体架构

- 代理层 （根据配置生成不同的动态代理类）。
- 路由层 （根据配置选用不同的负载均衡方法）。
- 注册中心层（根据配置接入多种注册中心，通过引入第三者来实现“协调”的效果）。
- 序列化层（根据配置采用不同的序列化框架，传输协议的统一）。

<img src="https://oss.zhulinz.top/newImage202302281408658.png" alt="image-20230228140847557" width="60%" />

### 7.1、责任链模式的意义

**使用责任链模式的好处：**

- 发送方与接收方的处理对象类之间解耦
- 封装每个处理对象，处理类的最小封装原则
- 可以任意添加处理对象，调整处理对象之间的顺序，提高了维护性和可拓展性，可以根据需求新增处理类，满足开闭原则。
- 增强了对象职责指派的灵活性，当流程发生变化的时候，可以动态地改变链内的调动次序可动态的新增或者删除。
- 责任链简化了对象之间的连接。每个对象只需保持一个指向其后继者的引用，不需保持其他所有处理者的引用，这避免了使用众多的 if 或者 if···else 语句。
- 责任分担。每个类只需要处理自己该处理的工作，不该处理的传递给下一个对象完成，明确各类的责任范围，符合类的单一职责原则。

1. **对客户端请求进行鉴权**

   客户端请求的远程接口可能需要进行权限校验（比如与用户隐私相关的数据），服务端必须确认该请求合法才可放行。

   **实现逻辑**：请求抵达服务端调用具体方法之前，先对调用凭证进行判断，如果凭证不一致则抛出异常

   ```java
   String clientToken = String.valueOf(rpcInfoContent.getAttachments().get("serverToken"));
   RpcServiceWrapper rpcServiceWrapper =
           CommonServerCache.PROVIDER_SERVICE_WRAPPER_MAP.get(rpcInfoContent.getTargetServiceName());
   String serviceToken = rpcServiceWrapper.getServiceToken();
   if (CommonUtil.isEmpty(serviceToken)) {
       return;
   }
   if (!CommonUtil.isEmpty(clientToken) && clientToken.equals(serviceToken)) {
       return;
   }
   throw new RuntimeException("clientToken is " + clientToken + " , verify is false");
   ```

2. **分组管理服务**

   同一个服务可能存在多个分支，有的分支为dev代表正处于开发阶段，有的分支为test代表正在处于测试阶段。

   为了避免客户端调用到正在开发中的服务，在进行远程调用时，需要根据group进行过滤。

3. **基于ip直连方式访问服务端**

   可能存在两个名字相同但代码逻辑不同的服务。为了避免出现不同的结果，需要根据服务提供方的IP进行过滤。

4. **调用过程中记录日志信息**

传统模式下，客户端在发送请求之前，逐个的调用过滤请求的方法；服务端在接受请求之前，也需要逐个调用过滤请求的方法。在该模式下，代码耦合度高，且扩展性差。

### 7.2、责任链模式设计

```
├── client
│   ├── ClientFilterChain.java					-> 客户端过滤链设计
│   └── impl
│       ├── ClientGroupFilterImpl.java			-> 客户端分组过滤实现类
│       ├── ClientLogFilterImpl.java			-> 客户端日志记录实现类
│       └── DirectInvokeFilterImpl.java			-> 客户端基于IP直连实现类
├── server
│   ├── impl
│   │   ├── ServerLogFilterImpl.java			-> 服务端日志记录实现类
│   │   └── ServerTokenFilterImpl.java			-> 服务端权限校验实现类
│   ├── ServerAfterFilterChain.java				-> 服务端后置过滤链
│   └── ServerBeforeFilterChain.java			-> 服务端前置过滤链						
├── ZClientFilter.java							-> 继承IFilter接口
├── ZFilter.java
└── ZServerFilter.java							-> 继承IFilter接口
```

1. 首先创建ZFilter接口，然后分别创建`服务器与客户端对应的接口`，继承ZFilter接口，代码解耦且方便后续扩展。

2. 责任链实现，分别创建服务器与客户端过滤链，用于存放过滤器实现类，并遍历过滤器实现类集合，执行过滤方法。

   ```java
   private static List<ZClientFilter> zClientFilters = new ArrayList<>();
   
   public void addZClientFilter(ZClientFilter zClientFilter) {
       zClientFilters.add(zClientFilter);
   }
   
   public void doFilter(List<ChannelFutureWrapper> src, RpcInfoContent rpcInfoContent) {
       for (ZClientFilter zClientFilter : zClientFilters) {
           zClientFilter.doFilter(src, rpcInfoContent);
       }
   }
   ```

3. 依次实现过滤器实现类，并通过SPI机制加载。

## 八、高并发利器--队列和多线程应用

### 8.1、串行同步阻塞问题

以下是服务端接收到客户端的请求信息后，对信息进行解码处理并去执行相应的方法，再将执行结果发送给客户端。

<img src="https://oss.zhulinz.top/newImage202302282042581.png" alt="image-20230228204203236" style="zoom:67%;" />

**Netty中NIO线程常见的阻塞情况**：

- 无意识：在ChannelHandler中编写了可能导致NIO线程阻塞的代码，但是用户没有意识到，包括但不限于查询各种数据库存储器的操作、第三方服务的远程调用、中间件服务的调用、等待锁等。
- 有意识：用户知道有耗时逻辑需要额外处理，但是在处理过程中翻车了，比如主动切换耗时逻辑到业务线程池或者业务的消息队列做处理时发生阻塞，最典型的有对方是阻塞队列，锁竞争激烈导致耗时，或者投递异步任务给消息队列时异机房的网络耗时，或者任务队列满了导致等待，等等。

在以上代码中，如果handler中存在比较耗时的操作（例如查询数据库）等，此时就可能会造成Netty的IO线程被长时间占用，出现线程堵塞情况，将会影响其他服务的远程调用。

### 8.2、异步设计

Netty的线程模型，通过设计了不同的线程池来管理不同的事件。workerGroup是负责服务端的read和write事件，bossGroup是负责accept事件。不同的线程池负责监听不同类型的事件。

<img src="https://oss.zhulinz.top/newImage202302282148558.png" alt="image-20230228214849470" width="60%" />

#### 使用堵塞队列提升吞吐性能

倘若当请求直接抵达服务器的时候我们就将数据丢入到业务线程池中，未免有些过于鲁莽，因为线程池的消费能力通常会和线程数有关。而线程数的配置通常又取决于CPU的核心数目。倘若要支撑1000次请求同时访问，这种设计很容易就会将线程池撑爆。为了尽量减少对线程池的压力，有以下几种手段：

1. **设置足够长的线程池队列**

   假设我们将线程池的内部队列设置过长，例如面对1000次并发请求，我们将队列长度设置到5000的长度，即服务端的超时极限为5秒，也就意味着一次请求的最糟糕耗时会有可能为5秒。另外一旦请求的数据被线程池提交，则意味着这个任务就无法取消了。面对一些处于堵塞状态的请求，市面上比较常见的处理手段，会在一定的时间段之后将其直接终止，返回给客户端，告知客户端当前服务器正处于繁忙阶段。

   除此之外，后续的一些熔断设计方案模块也需要考虑到倘若请求过多，是否能够将处于堆积的任务进行取消。

2. **单独设计一条独立的队列用于接收请求**

   单独使用一条堵塞队列用于接收请求，然后在队尾由业务线程池来负责消费请求数据。这样即使请求出现了堆积，也是堆积在一条我们比较能轻易操作的队列当中，相比于上一套方案的技术难度会有所降低。

#### 代码实现

在服务端的channelHandler中只进行将接收到的请求信息添加进一个阻塞队列中。

```java
#ServerReadHandler channelRead()
//服务端以统一的协议RpcProtocol接收数据
RpcProtocol rpcProtocol = (RpcProtocol) msg;
ServerChannelReadData serverChannelReadData = new ServerChannelReadData();
serverChannelReadData.setRpcProtocol(rpcProtocol);
serverChannelReadData.setCtx(ctx);
SERVER_CHANNEL_DISPATCHER.addData(serverChannelReadData);
```

另外启动一个线程池来获取阻塞队列中的数据，然后进行业务逻辑处理。

```java
class ServerJobCoreHandler implements Runnable {
    @Override
    public void run() {
        while (true) {
            try {
                ServerChannelReadData channelReadData = RPC_DATA_QUEUE.take();
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        //获取客户端请求的协议体
                        RpcProtocol rpcProtocol = channelReadData.getRpcProtocol();
                        //反序列化得到请求内容
                        RpcInfoContent rpcInfoContent = SERVER_SERIALIZE_FACTORY.deSerialize(RpcInfoContent.class, rpcProtocol.getContent());
                        //执行过滤链路
                        try {
                            //执行前置过滤器
                            SERVER_BEFORE_FILTER_CHAIN.doFilter(rpcInfoContent);
                        } catch (Exception cause) {
                            //捕捉异常信息
                            if (cause instanceof ZRpcException) {
                                ZRpcException zRpcException = (ZRpcException) cause;
                                rpcInfoContent.setE(zRpcException);
                                RpcProtocol respProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInfoContent));
                                channelReadData.getCtx().writeAndFlush(respProtocol);
                                return;
                            }
                        }
                        //在服务端暴露的提供服务集合中通过服务名获取服务
                        Object aimClass = PROVIDER_MAP.get(rpcInfoContent.getTargetServiceName());
                        //获取该服务的方法
                        Method[] methods = aimClass.getClass().getDeclaredMethods();
                        Object result = null;
                        //遍历方法，反射执行目标方法
                        for (Method method : methods) {
                            if (method.getName().equals(rpcInfoContent.getTargetMethod())) {
                                if (method.getReturnType().equals(Void.TYPE)) {
                                    try {
                                        method.invoke(aimClass, rpcInfoContent.getArgs());
                                    } catch (Exception e) {
                                        rpcInfoContent.setE(e);
                                    }
                                } else {
                                    try {
                                        result = method.invoke(aimClass, rpcInfoContent.getArgs());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        rpcInfoContent.setE(e);
                                    }
                                }
                                //跳出循环
                                break;
                            }
                        }
                        //写入响应数据
                        rpcInfoContent.setResponse(result);
                        //执行后置过滤器
                        SERVER_AFTER_FILTER_CHAIN.doFilter(rpcInfoContent);
                        RpcProtocol respProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInfoContent));
                        //给客户端发送响应数据
                        channelReadData.getCtx().writeAndFlush(respProtocol);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

## 九、容错设计

### 9.1、报错日志打印

在客户端进行远程调用服务时，而此时服务端所调用的方法出现异常Exception。如果异常只记录在服务端中：

- 无法区分哪些异常是由哪些客户端发出的请求所致。
- 客户端调用接口获取不到预期数据提示调用错误，但是异常堆栈信息记录在服务端的机器上，异常排查困难。
- 服务端的错误日志堆积远大于调用方，比较消耗磁盘空间。

**将服务端的异常信息统一采集起来，返回给到调用方并且将堆栈记录打印。**

服务端在反射调用方法时，通过try~catch()捕获该此调用的异常信息。

```java
if (method.getReturnType().equals(Void.TYPE)) {
    try {
        method.invoke(aimClass, rpcInfoContent.getArgs());
    } catch (Exception e) {
        rpcInfoContent.setE(e);
    }
} else {
    try {
        result = method.invoke(aimClass, rpcInfoContent.getArgs());
    } catch (Exception e) {
        e.printStackTrace();
        rpcInfoContent.setE(e);
    }
}
```

客户端在接收到响应信息后判断是否有异常信息。

```java
//判断是否有异常信息
if (rpcInfoContent.getE() != null) {
    rpcInfoContent.getE().printStackTrace();
}
```

**异常信息传输问题**

网络传输数据的时候是需要对数据包进行编解码，而异常的堆栈信息通常会携带非常多的文本记录。此时就会出现一个问题，服务端发送的信息字节数为2580B，而客户端接收到的信息字节数只有2048B。导致这个问题的原因就是传输的数据体积过大，TCP的一份数据包被拆解成了多份进行传输。

解决方法：规定传输消息长度，防止粘包与半包现象。

```java
public class RpcProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {
    public RpcProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                                   int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    public RpcProtocolFrameDecoder() {
        this(10*1024, 2, 4, 0, 0);
    }
}
```

### 9.2、超时重试机制

并不是所有的接口在超时的时候都需要进行重试，面对一些非幂等性的接口调用情况。

**适合使用重试机制的场景：**

- 目标集群中有A、B服务器，A服务器性能不佳，处理请求比较缓慢，B服务器性能优于A，所以当接口调用A出现超时之后，可以尝试重新发起调用，将请求转到B上从而获取数据结果。
- 网络由于某些特殊异常，导致突然间断，此时可以通过重试机制发起二次调用，重试机制对接口的整体可用性也有了一定的保障性。

**不适合使用重试机制的场景：**

- 对于一些对数据重复性较为敏感的接口，例如转账、下单，以及一些和金融相关的接口，当接口调用出现超时之后，并不好确认数据包是否已经抵达到目标服务。

**逻辑实现：**

通过在调用服务的参数中携带一个`retry`参数，这个参数的作用是在客户端等待响应的过程中，如果出现了超时情况，则会自动发起二次重试的功能（注意，一般发起二次重试的时候尽量不要选择同一台机器进行重试）。

```java
//判断是否超时 或者 是否设置了重试机制
Boolean isNotTimeOut = timeoutInvocation.tryAcquire(timeOut, TimeUnit.MILLISECONDS);
if (isNotTimeOut || rpcInfoContent.getRetry() > 0) {
    RpcInfoContent respInfo =
            ((TimeoutInvocation) RESP_MAP.remove(rpcInfoContent.getUuid())).getRpcInfoContent();
    if (isNotTimeOut && respInfo.getE() == null) {
        //为超时且没有异常信息
        return respInfo.getResponse();
    } else if (respInfo.getRetry() > 0) {
        //重试次数++
        retryTimes++;
        //重试，重新设置UUID
        //rpcInfoContent.setUuid(UUID.randomUUID().toString());
        respInfo.setResponse(null);
        respInfo.setE(null);
        respInfo.setRetry(respInfo.getRetry() - 1);
        //回调接收重试之后的响应数据
        return tryFinishedTask(respInfo, rpcReferenceWrapper);
    } else if (respInfo.getE() != null) {
        respInfo.getE().printStackTrace();
    }
}
```

<img src="https://oss.zhulinz.top/newImage202302282301636.png" alt="image-20230228230104522" width="50%" />

### 9.3、服务端保护机制

限制服务端的总体连接数，超过指定连接数时，拒绝剩余的连接请求。

Netty线程模型是采用了主从Reactors的多线程模型设计，基本划分为了：

- MainReactor负责客户端的连接请求，将请求转发给SubReactor。
- SubReactor负责相关通道的IO读写信息。
- 业务逻辑部分单独抽离出来交给了业务线程池处理。

限制建立连接这部分的事件处理，应该由MainReactor处理

<img src="https://oss.zhulinz.top/newImage202302282308622.png" alt="屏幕截图 2023-02-28 230459" width="60%" />

```java
System.out.println("connection limit handler");
Channel channel = (Channel) msg;
int conn = numConnection.incrementAndGet();
if (conn > 0 && conn <= maxConnectionNum) {
    this.childChannel.add(channel);
    channel.closeFuture().addListener(future -> {
        childChannel.remove(channel);
        numConnection.decrementAndGet();
    });
    super.channelRead(ctx, msg);
} else {
    numConnection.decrementAndGet();
    //避免产生大量的time_wait连接
    channel.config().setOption(ChannelOption.SO_LINGER, 0);
    channel.unsafe().closeForcibly();
    numDroppedConnections.increment();
    //这里加入一道cas可以减少一些并发请求的压力,定期地执行一些日志打印
    if (loggingScheduled.compareAndSet(false, true)) {
        ctx.executor().schedule(this::writeNumDroppedConnectionLog, 1, TimeUnit.SECONDS);
    }
}
```

### 9.4、服务端限流策略

主要采用`Semaphore`的组件进行实现，`Semaphore`是一款由JDK提供的专门用于**限制并发访问特定资源线程数的组件**，它提供了`acquire`和`tryAcquire`两种方法供开发者调用，是`Semaphore`的内部其实是有一个计数器，每次向它申请许可的时候如果计数器不为0，则申请通过，如果计数器为0则会处于`堵塞（acquire）`，或者`立马断开（tryAcquire）`，又或者在等待一定时间后才断开（tryAcquire可以指定等待时间）。当资源使用完毕之后需要执行`release`操作，将计数器归还。

![image-20230313213849530](https://oss.zhulinz.top/newImage202303132138628.png)

**限时流采用acquire合理吗？**

使用`acquire`时，如果许可书减少为0则会堵塞当前调用线程，让客户端处于等待队列，在面对大量并发的访问容易造成整体接口的平均响应时间越来越大，导致整个服务的吞吐率越来越低。

并且当有大量请求因为`acquire`处于堵塞状态停留在服务端内存中的时候，容易导致内存上升，从而产生出现`频繁gc升至oom异常`。

**代码实现**

使用tryAcquire则是一种“快速响应”的解决思路，当获取申请失败后，不会堵塞当前线程，而是立马通知客户端调用异常，然后发起二次重试，路由到其他节点。

在服务注册时，初始化服务的限流次数，在缓存中根据服务名存储一个`Semaphore`对象。

![屏幕截图 2023-03-13 215720](https://oss.zhulinz.top/newImage202303132158116.png)

在服务端的前置过滤链中取服务名对应的`Semaphore`对象，调用`tryAcquire()`方法，判断是否可调用，否的话直接向客户端返回错误信息。

```java
@Slf4j
@SPI("before")
public class ServerServiceBeforeFilterImpl implements ZServerFilter {
    @Override
    public void doFilter(RpcInfoContent rpcInfoContent) {
        String serviceName = rpcInfoContent.getTargetServiceName();
        ServiceSemaphoreWrapper serviceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE.get(serviceName);
        //从缓存中提取semaphore对象
        Semaphore semaphore = serviceSemaphoreWrapper.getSemaphore();
        boolean tryResult = semaphore.tryAcquire();
        if (!tryResult) {
            String errorMsg =
                    rpcInfoContent.getTargetServiceName() + "'s max request is " + serviceSemaphoreWrapper.getMaxNums() + ",reject now";
            throw new MaxLimitException(errorMsg);
        }
    }
}
```

业务处理完毕后，向客户端返回服务调用结果，并且在后置过滤链中释放`Semaphore`资源。

```java
@SPI("after")
public class ServerServiceAfterFilterImpl implements ZServerFilter {
    @Override
    public void doFilter(RpcInfoContent rpcInfoContent) {
        String serviceName = rpcInfoContent.getTargetServiceName();
        ServiceSemaphoreWrapper serviceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE.get(serviceName);
        serviceSemaphoreWrapper.getSemaphore().release();
    }
}
```

## 十、接入SpringBoot框架层

### 10.1、如何自定义SpringBootStarter

**Starter加载原理**

SpringBoot项目的启动类都会有一个注解`@SpringBootApplication`，在项目启动的时候，会将项目中所有声明为Bean对象（注解、xml）的实例信息全部加载到ioc容器中。除此之外也会将所有依赖到的starter里的bean信息加载到ioc容器中，从而做到零配置、开箱即用。

可以`@SpringBootApplication`注解里看到有个`@EnableAutoConfiguration`注解，正是通过该注解来加载starter。

![image-20230309232332558](https://oss.zhulinz.top/newImage202303092323764.png)

具体的实现是在`@EnableAutoConfiguration`注解下`import`了一个`AutoConfigurationImportSelector`加载器实现。

![image-20230309232925464](https://oss.zhulinz.top/newImage202303092329561.png)

AutoConfigurationImportSelector类是通过利用SpringBoot的SPI机制获取`org.springframework.boot.autoconfigure.EnableAutoConfiguration`的实现类，而SPI机制是在文件`spring.factories`中获取实现类的全限定名。

![image-20230309234823198](https://oss.zhulinz.top/newImage202303092348297.png)

### 10.2、代码实现

新建starter模块，引入依赖

![image-20230313185449705](https://oss.zhulinz.top/newImage202303131854948.png)

**服务端注解**

通过注解暴露服务，并配置一些服务基本信息

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ZRpcServer {
    int limit() default 0;

    String group() default "default";

    String serviceToken() default "";
}
```

**客户端注解**

通过注解配置服务消费者

```java
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
```

**服务端自动配置类**

服务提供者使用@ZRpcService注解，表示该类需要被暴露出去被外界调用，该配置类就是在容器启动环节中，将带有@ZRpcService注解的类给注入到容器内部。

```java
public class ZRpcServerAutoConfiguration implements InitializingBean, ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        RpcServer rpcServer = null;
        //获取有该注解的类
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(ZRpcServer.class);
        if (beanMap.size() == 0) {
            return;
        }
        //输出项目启动信息
        //printBanner();
        long beginTime = System.currentTimeMillis();
        rpcServer = new RpcServer();
        rpcServer.initServerConfig();
        ZRpcListenerLoader zRpcListenerLoader = new ZRpcListenerLoader();
        zRpcListenerLoader.init();
        //开始暴露服务
        for (String beanName : beanMap.keySet()) {
            Object bean = beanMap.get(beanName);
            //获取服务的注解信息
            ZRpcServer zRpcServer = bean.getClass().getDeclaredAnnotation(ZRpcServer.class);
            //构建服务注册信息
            RpcServiceWrapper rpcServiceWrapper = new RpcServiceWrapper(bean, zRpcServer.group());
            rpcServiceWrapper.setServiceToken(zRpcServer.serviceToken());
            rpcServiceWrapper.setLimit(zRpcServer.limit());
            rpcServer.registryService(rpcServiceWrapper);
            log.info(">>>>>>>>>>>>> [zrpc] {} registry success >>>>>>>>>>>>>", beanName);
        }
        long endTime = System.currentTimeMillis();
        //启动监听服务注销线程
        ServerShutDownHook.registryShutdownHook();
        //启动netty服务端
        rpcServer.startApplication();
        log.info("============= [{}] started success in {}s =============",
                CommonServerCache.SERVER_CONFIG.getApplicationName(), ((double) endTime - (double) beginTime) / 1000);

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
```

**客户端自动配置类**

在Spring容器启动多过程中，将这些个带有` @ZRpcReference注解`的字段进行构建，让它们的句柄可以指向一个代理类（也就是我们前期代理层里生成的代理对象），这样在使用UserService和OrderService类对应的方法时候就会感觉到似乎在执行本地调用一样，降低开发者的代码编写难度。

```java
public class ZRpcClientAutoConfiguration implements BeanPostProcessor, ApplicationListener<ApplicationReadyEvent> {
    private static RpcReference rpcReference;
    private static RpcClient rpcClient = null;
    private volatile boolean needInitClient = false;
    private volatile boolean hasInitClientConfig = false;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //获取bean的属性
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            //判断属性是否有ZRpcReference注解
            if (field.isAnnotationPresent(ZRpcReference.class)) {
                //判断客户端配置文件是否初始化
                if (!hasInitClientConfig) {
                    rpcClient = new RpcClient();
                    try {
                        //获取代理对象
                        rpcReference = rpcClient.initApplication();
                    } catch (Exception e) {
                        log.error("[IRpcClientAutoConfiguration] postProcessAfterInitialization has error ", e);
                        throw new RuntimeException(e);
                    }
                    hasInitClientConfig = true;
                }
                needInitClient = true;
                //获取注解信息
                ZRpcReference zRpcReference = field.getDeclaredAnnotation(ZRpcReference.class);
                try {
                    //通过setAccessible(true)的方式关闭安全检查就可以达到提升反射速度的目的
                    field.setAccessible(true);
                    //获取属性的值
                    Object refObj = field.get(bean);
                    //构建调用服务的请求信息
                    RpcReferenceWrapper rpcReferenceWrapper = new RpcReferenceWrapper();
                    //field.getType()获取属性的类型
                    rpcReferenceWrapper.setAimClass(field.getType());
                    rpcReferenceWrapper.setGroup(zRpcReference.group());
                    rpcReferenceWrapper.setServiceToken(zRpcReference.serviceToken());
                    rpcReferenceWrapper.setUrl(zRpcReference.url());
                    rpcReferenceWrapper.setTimeOut(zRpcReference.timeOut());
                    rpcReferenceWrapper.setRetry(zRpcReference.retry());
                    rpcReferenceWrapper.setAsync(zRpcReference.async());
                    //订阅服务
                    rpcClient.doSubscribeService(field.getType());
                    //通过代理获取代理对象
                    refObj = rpcReference.get(rpcReferenceWrapper);
                    field.set(bean, refObj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return bean;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (needInitClient && rpcClient != null) {
            log.info("============== [{}] started success ==============",
                    CommonClientCache.CLIENT_CONFIG.getApplicationName());
            ConnectionHandler.bootstrap = rpcClient.getBootstrap();
            rpcClient.doConnectServer();
            rpcClient.startSendMsg();
        }
    }
}
```



## 十一、性能测试

### 11.1、序列化测试

**测试代码**

```java
private static User buildRpcInfo() {
    User user=new User();
    user.setId(1);
    user.setName("zhfwafscawedqwdulin");
    user.setBankNo("2314141wafawdfwafawd2412512421");
    user.setAge(20);
    user.setSex(1);
    user.setRemark("hjdjskcvjakosjwfszacqwdsq   wdqwdqwdpdjqpoawdjk");
    return user;
}

@Benchmark
public void fastJsonTest() {
    SerializeFactory serializeFactory = new FastJsonSerializeFactory();
    User rpcInfoContent = buildRpcInfo();
    byte[] serialize = serializeFactory.serialize(rpcInfoContent);
    User serialRpc = serializeFactory.deSerialize(User.class, serialize);
}

@Benchmark
public void hessianTest() {
    SerializeFactory serializeFactory = new HessianSerializeFactory();
    User rpcInfoContent = buildRpcInfo();
    byte[] serialize = serializeFactory.serialize(rpcInfoContent);
    User serialRpc = serializeFactory.deSerialize(User.class, serialize);
}

@Benchmark
public void jdkTest() {
    SerializeFactory serializeFactory = new JDKSerializeFactory();
    User rpcInfoContent = buildRpcInfo();
    byte[] serialize = serializeFactory.serialize(rpcInfoContent);
    User serialRpc = serializeFactory.deSerialize(User.class, serialize);
}

@Benchmark
public void kryoTest() {
    SerializeFactory serializeFactory = new KryoSerializeFactory();
    User rpcInfoContent = buildRpcInfo();
    byte[] serialize = serializeFactory.serialize(rpcInfoContent);
    User serialRpc = serializeFactory.deSerialize(User.class, serialize);
}

@Benchmark
public void protostuffTest() {
    SerializeFactory serializeFactory = new ProtostuffSerializeFactory();
    User rpcInfoContent = buildRpcInfo();
    byte[] serialize = serializeFactory.serialize(rpcInfoContent);
    User serialRpc = serializeFactory.deSerialize(User.class, serialize);
}

public static void main(String[] args) throws RunnerException {
    //配置进行2轮热数 测试2轮 1个线程
    //预热的原因 是JVM在代码执行多次会有优化
    Options options = new OptionsBuilder().warmupIterations(2).measurementBatchSize(2)
            .forks(1).build();
    new Runner(options).run();
}
```

**测试结果**

```txt
# JMH version: 1.36
# VM version: JDK 1.8.0_361, Java HotSpot(TM) 64-Bit Server VM, 25.361-b09
# VM invoker: D:\MyApp\Java\jdk_1.8.0\jre\bin\java.exe
# VM options: -javaagent:D:\MyApp\Java\JetBrains\apps\IDEA-U\ch-0\213.7172.25\lib\idea_rt.jar=58488:D:\MyApp\Java\JetBrains\apps\IDEA-U\ch-0\213.7172.25\bin -Dfile.encoding=UTF-8
# Blackhole mode: full + dont-inline hint (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)
# Warmup: 2 iterations, 10 s each
# Measurement: 5 iterations, 10 s each, 2 calls per op
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.zhulin.test.SerializFactoryTest.fastJsonTest

# Run progress: 0.00% complete, ETA 00:05:50
# Fork: 1 of 1
# Warmup Iteration   1: 937336.017 ops/s
# Warmup Iteration   2: 1013619.112 ops/s
Iteration   1: 501767.470 ops/s
Iteration   2: 507629.606 ops/s
Iteration   3: 506139.619 ops/s
Iteration   4: 498861.173 ops/s
Iteration   5: 501283.056 ops/s


Result "com.zhulin.test.SerializFactoryTest.fastJsonTest":
  503136.185 ±(99.9%) 13989.375 ops/s [Average]
  (min, avg, max) = (498861.173, 503136.185, 507629.606), stdev = 3632.996
  CI (99.9%): [489146.810, 517125.560] (assumes normal distribution)


# JMH version: 1.36
# VM version: JDK 1.8.0_361, Java HotSpot(TM) 64-Bit Server VM, 25.361-b09
# VM invoker: D:\MyApp\Java\jdk_1.8.0\jre\bin\java.exe
# VM options: -javaagent:D:\MyApp\Java\JetBrains\apps\IDEA-U\ch-0\213.7172.25\lib\idea_rt.jar=58488:D:\MyApp\Java\JetBrains\apps\IDEA-U\ch-0\213.7172.25\bin -Dfile.encoding=UTF-8
# Blackhole mode: full + dont-inline hint (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)
# Warmup: 2 iterations, 10 s each
# Measurement: 5 iterations, 10 s each, 2 calls per op
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.zhulin.test.SerializFactoryTest.hessianTest

# Run progress: 20.00% complete, ETA 00:04:45
# Fork: 1 of 1
# Warmup Iteration   1: 256979.968 ops/s
# Warmup Iteration   2: 274301.852 ops/s
Iteration   1: 136895.761 ops/s
Iteration   2: 133357.619 ops/s
Iteration   3: 137209.522 ops/s
Iteration   4: 136348.419 ops/s
Iteration   5: 137882.603 ops/s


Result "com.zhulin.test.SerializFactoryTest.hessianTest":
  136338.785 ±(99.9%) 6763.123 ops/s [Average]
  (min, avg, max) = (133357.619, 136338.785, 137882.603), stdev = 1756.362
  CI (99.9%): [129575.662, 143101.908] (assumes normal distribution)


# JMH version: 1.36
# VM version: JDK 1.8.0_361, Java HotSpot(TM) 64-Bit Server VM, 25.361-b09
# VM invoker: D:\MyApp\Java\jdk_1.8.0\jre\bin\java.exe
# VM options: -javaagent:D:\MyApp\Java\JetBrains\apps\IDEA-U\ch-0\213.7172.25\lib\idea_rt.jar=58488:D:\MyApp\Java\JetBrains\apps\IDEA-U\ch-0\213.7172.25\bin -Dfile.encoding=UTF-8
# Blackhole mode: full + dont-inline hint (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)
# Warmup: 2 iterations, 10 s each
# Measurement: 5 iterations, 10 s each, 2 calls per op
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.zhulin.test.SerializFactoryTest.jdkTest

# Run progress: 40.00% complete, ETA 00:03:34
# Fork: 1 of 1
# Warmup Iteration   1: 135154.965 ops/s
# Warmup Iteration   2: 143723.302 ops/s
Iteration   1: 72387.023 ops/s
Iteration   2: 73093.665 ops/s
Iteration   3: 72454.358 ops/s
Iteration   4: 72922.510 ops/s
Iteration   5: 72247.679 ops/s


Result "com.zhulin.test.SerializFactoryTest.jdkTest":
  72621.047 ±(99.9%) 1409.836 ops/s [Average]
  (min, avg, max) = (72247.679, 72621.047, 73093.665), stdev = 366.130
  CI (99.9%): [71211.211, 74030.883] (assumes normal distribution)


# JMH version: 1.36
# VM version: JDK 1.8.0_361, Java HotSpot(TM) 64-Bit Server VM, 25.361-b09
# VM invoker: D:\MyApp\Java\jdk_1.8.0\jre\bin\java.exe
# VM options: -javaagent:D:\MyApp\Java\JetBrains\apps\IDEA-U\ch-0\213.7172.25\lib\idea_rt.jar=58488:D:\MyApp\Java\JetBrains\apps\IDEA-U\ch-0\213.7172.25\bin -Dfile.encoding=UTF-8
# Blackhole mode: full + dont-inline hint (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)
# Warmup: 2 iterations, 10 s each
# Measurement: 5 iterations, 10 s each, 2 calls per op
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.zhulin.test.SerializFactoryTest.kryoTest

# Run progress: 60.00% complete, ETA 00:02:22
# Fork: 1 of 1
# Warmup Iteration   1: 447546.264 ops/s
# Warmup Iteration   2: 473237.006 ops/s
Iteration   1: 238733.303 ops/s
Iteration   2: 238312.951 ops/s
Iteration   3: 237201.772 ops/s
Iteration   4: 236283.143 ops/s
Iteration   5: 238182.096 ops/s


Result "com.zhulin.test.SerializFactoryTest.kryoTest":
  237742.653 ±(99.9%) 3813.139 ops/s [Average]
  (min, avg, max) = (236283.143, 237742.653, 238733.303), stdev = 990.260
  CI (99.9%): [233929.514, 241555.792] (assumes normal distribution)


# JMH version: 1.36
# VM version: JDK 1.8.0_361, Java HotSpot(TM) 64-Bit Server VM, 25.361-b09
# VM invoker: D:\MyApp\Java\jdk_1.8.0\jre\bin\java.exe
# VM options: -javaagent:D:\MyApp\Java\JetBrains\apps\IDEA-U\ch-0\213.7172.25\lib\idea_rt.jar=58488:D:\MyApp\Java\JetBrains\apps\IDEA-U\ch-0\213.7172.25\bin -Dfile.encoding=UTF-8
# Blackhole mode: full + dont-inline hint (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)
# Warmup: 2 iterations, 10 s each
# Measurement: 5 iterations, 10 s each, 2 calls per op
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.zhulin.test.SerializFactoryTest.protostuffTest

# Run progress: 80.00% complete, ETA 00:01:11
# Fork: 1 of 1
# Warmup Iteration   1: 1708233.542 ops/s
# Warmup Iteration   2: 1758588.963 ops/s
Iteration   1: 894368.413 ops/s
Iteration   2: 876232.239 ops/s
Iteration   3: 879603.455 ops/s
Iteration   4: 874135.469 ops/s
Iteration   5: 871293.512 ops/s


Result "com.zhulin.test.SerializFactoryTest.protostuffTest":
  879126.618 ±(99.9%) 34825.783 ops/s [Average]
  (min, avg, max) = (871293.512, 879126.618, 894368.413), stdev = 9044.145
  CI (99.9%): [844300.835, 913952.401] (assumes normal distribution)


# Run complete. Total time: 00:05:56

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

Benchmark                            Mode  Cnt       Score       Error  Units
SerializFactoryTest.fastJsonTest    thrpt    5  503136.185 ± 13989.375  ops/s
SerializFactoryTest.hessianTest     thrpt    5  136338.785 ±  6763.123  ops/s
SerializFactoryTest.jdkTest         thrpt    5   72621.047 ±  1409.836  ops/s
SerializFactoryTest.kryoTest        thrpt    5  237742.653 ±  3813.139  ops/s
SerializFactoryTest.protostuffTest  thrpt    5  879126.618 ± 34825.783  ops/s
```

### 11.2、AB压测

**ab参数介绍：**

```txt
格式：ab [options] [http://]hostname[:port]/path

下面是参数

-n requests Number of requests to perform             //本次测试发起的总请求数
-c concurrency Number of multiple requests to make　　 //一次产生的请求数（或并发数）
-t timelimit Seconds to max. wait for responses　　　　//测试所进行的最大秒数，默认没有时间限制。
-r Don't exit on socket receive errors.              // 抛出异常继续执行测试任务 
-p postfile File containing data to POST　　    //包含了需要POST的数据的文件，文件格式如“p1=1&p2=2”.使用方法是 -p 111.txt

-T content-type Content-type header for POSTing
// POST 数据所使用的 Content-type 头信息，如 -T “application/x-www-form-urlencoded” 。 （配合-p）

-v verbosity How much troubleshooting info to print
//设置显示信息的详细程度 – 4或更大值会显示头信息， 3或更大值可以显示响应代码(404, 200等), 2或更大值可以显示警告和其他信息。 -V 显示版本号并退出。

-C attribute Add cookie, eg. -C “c1=1234,c2=2,c3=3” (repeatable)

//-C cookie-name=value 对请求附加一个Cookie:行。 其典型形式是name=value的一个参数对。此参数可以重复，用逗号分割。
提示：可以借助session实现原理传递 JSESSIONID参数， 实现保持会话的功能，如-C ” c1=1234,c2=2,c3=3, JSESSIONID=FF056CD16DA9D71CB131C1D56F0319F8″ 。

-w Print out results in HTML tables　　//以HTML表的格式输出结果。默认时，它是白色背景的两列宽度的一张表。
-i Use HEAD instead of GET
```

**请求数10000  并发量1**

ab -n 10000 -c 1 http://localhost:8080/user/sayHello?msg=zhulin

<img src="https://oss.zhulinz.top/newImage202303012354836.png" alt="image-20230301235429683" width="60%" />

Requests per second: 584.80 [#/sec] (mean)<br>**//吞吐率，大家最关心的指标之一，相当于 LR 中的每秒事务数，后面括号中的 mean 表示这是一个平均值**

Time per request: 1.710 [ms] (mean)<br>
**//用户平均请求等待时间，大家最关心的指标之二，相当于 LR 中的平均事务响应时间，后面括号中的 mean 表示这是一个平均值**

Time per request: 1.710 [ms] (mean, across all concurrent requests)<br>
**//服务器平均请求处理时间，大家最关心的指标之三**



```docker
docker run -d --name ddns-go --restart=always --net=host -v /www/wwwroot/ddns-go:/root jeessy/ddns-go
```



