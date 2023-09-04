参考自[Java开发者的RPC实战课](https://juejin.cn/book/7047357110337667076)


核心组件
- Server
- Client
- Server Stub
- Client Stub

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/671899dc974749f685f0c42f683730d7~tplv-k3u1fbpfcp-jj-mark:1512:0:0:0:q75.awebp)

![RPC接入Spring](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/88323654f3974b8fa9b5da4ae7c8e950~tplv-k3u1fbpfcp-jj-mark:1512:0:0:0:q75.avis)


## 代理层
对于远程方法调用，将内部的细节进行封装屏蔽，使得方法用起来就像本地方法调用一样方便

代理的好处：
 - 在客户端与目标对象之间起到中介作用和保护目标对象的作用
 - 代理对象可与扩展目标对象的恩公鞥
 - 将客户端与目标兑现分离，在一定程度上降低系统的耦合度

### 网络连接方法
使用Netty实现服务提供者节点和客户端的连接，需要注意的基本是handler的设计:
```java
    bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            System.out.println("初始化provider过程");
            ch.pipeline().addLast(new RpcEncoder());
            ch.pipeline().addLast(new RpcDecoder());
            ch.pipeline().addLast(new ServerHandler());
        }
    });
```
在这里，实现了编码器和解码器以及具体处理方法的实现：
```java
/**
 * RPC请求编码器
 */
public class RpcEncoder extends MessageToByteEncoder<RpcProtocol> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcProtocol msg, ByteBuf out) throws Exception {
        out.writeShort(msg.getMagicNumber());
        out.writeInt(msg.getContentLength());
        out.writeBytes(msg.getContent());
    }
}

```
```java
/**
 * RPC解码器，需要考虑战粘包拆包问题，以及设置请求数据包体积最大值
 */
public class RpcDecoder extends ByteToMessageDecoder {
    /**
     * 协议开头部分的标准长度
     */
    public final int BASE_LENGTH = 2 + 4;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        if (byteBuf.readableBytes() >= BASE_LENGTH) {
            if (byteBuf.readShort() != MAGIC_NUMBER) {
                // 不是魔数开头，说明是非法的客户端发来的数据包
                ctx.close();
                return;
            }
            // RpcProtocol的contentLength字段
            int length = byteBuf.readInt();
            if (byteBuf.readableBytes() < length) {
                // 长度不匹配，说明数据包不完整
                ctx.close();
                return;
            }
            // 解析content字段
            byte[] data = new byte[length];
            byteBuf.readBytes(data);
            RpcProtocol protocol = new RpcProtocol(data);
            out.add(protocol);
        }
    }
}
```
接着是server和client的具体处理handler,`ServerHandler`的主要职责是解析协议，然后找到远程调用函数对应的代理对象，再将结果返回
```java
/**
 * 服务器接收到数据后的处理类
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {
    /**
     * 根据收到的信息解析出RpcInvocation，即远程调用方法信息，并通过反射的方式调用响应方法，拿到结果并返回
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ServerChannelReadData serverChannelReadData = new ServerChannelReadData();
        serverChannelReadData.setRpcProtocol((RpcProtocol) msg);
        serverChannelReadData.setChannelHandlerContext(ctx);
        // 放入channel分发器
        SERVER_CHANNEL_DISPATCHER.add(serverChannelReadData);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            ctx.close();
        }
    }
}
```
其中`ServerChannelDispatcher`封装了一个线程池，用于处理具体事务，类似reactor模式，核心处理代码如下：
```java
    ServerChannelReadData serverChannelReadData = RPC_DATA_QUEUE.take();
    executorService.execute(() -> {
        RpcProtocol rpcProtocol = serverChannelReadData.getRpcProtocol();
        RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(), RpcInvocation.class);
        System.out.println("rpcInvocation:" + rpcInvocation.getTargetServiceName());
        System.out.println("serialize:" + SERVER_SERIALIZE_FACTORY);
        // 执行过滤请求
        try {
            SERVER_BEFORE_FILTER_CHAIN.doFilter(rpcInvocation);
        } catch (Exception e) {
            if (e instanceof RpcException) {
                RpcException rpcException = (RpcException) e;
                RpcInvocation repParam = rpcException.getRpcInvocation();
                rpcInvocation.setE(e);
                byte[] body = SERVER_SERIALIZE_FACTORY.serialize(repParam);
                RpcProtocol respRpcProtocol = new RpcProtocol(body);
                serverChannelReadData.getChannelHandlerContext().writeAndFlush(respRpcProtocol);
                return;
            }
        }
        // 从服务提供者中获取目标服务
        Object aimObject = PROVIDER_CLASS_MAP.get(rpcInvocation.getTargetServiceName());
        // 获取目标服务的全部方法
        Method[] methods = aimObject.getClass().getDeclaredMethods();
        Object result = null;
        // 寻找得到对应的并执行
        for (Method method : methods) {
            try {
                if (method.getName().equals(rpcInvocation.getTargetMethod())) {
                    if (method.getReturnType().equals(Void.TYPE)) {
                        method.invoke(aimObject, rpcInvocation.getArgs());
                    } else {
                        result = method.invoke(aimObject, rpcInvocation.getArgs());
                    }
                    break;
                }
            } catch (Exception e) {
                rpcInvocation.setE(e);
            }
            // 设置response
            rpcInvocation.setResponse(result);
            // 后置过滤器
            SERVER_AFTER_FILTER_CHAIN.doFilter(rpcInvocation);
            // 再次封装为RpcProtocol返回给客户端
            RpcProtocol respRpcProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInvocation));
            serverChannelReadData.getChannelHandlerContext().writeAndFlush(respRpcProtocol);
        }
    });
```


`ClientHandler`的主要职责则是验证server的返回值并返回给远程调用方法
```java
/**
 * 客户端收到数据后的处理类
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        byte[] reqContent = rpcProtocol.getContent();
        String json = new String(reqContent);
        RpcInvocation rpcInvocation = JSON.parseObject(json, RpcInvocation.class);
        // 通过uuid来注入匹配的响应数值
        if (!RESP_MAP.containsKey(rpcInvocation.getUuid())) {
            throw new IllegalArgumentException("server response is error!");
        }
        // 将请求的响应结构放入一个Map集合中，集合的key就是uuid，这个uuid在发送请求之前就已经初始化好了，所以只需要起一个线程在后台遍历这个map，查看对应的key是否有相应即可。
        RESP_MAP.put(rpcInvocation.getUuid(), rpcInvocation);
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            ctx.close();
        }
    }
}
```

### 代理方法
接着是代理方法的创建，这里定义了接口`ProxyFactory`，同时给出了该接口的JDK和jassist两种实现
```java
public interface ProxyFactory {
    <T> T getProxy(final Class clazz) throws Exception;
}
```
JDK实现的主要代码如下，本质是封装成`RpcInvocation`并发送给server，然后返回server传过来的数据
```java
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setArgs(args);
        rpcInvocation.setTargetMethod(method.getName());
        rpcInvocation.setTargetServiceName(clazz.getName());
        //这里面注入了一个uuid，对每一次的请求都做单独区分
        rpcInvocation.setUuid(UUID.randomUUID().toString());
        RESP_MAP.put(rpcInvocation.getUuid(), OBJECT);
        //这里就是将请求的参数放入到发送队列中
        SEND_QUEUE.add(rpcInvocation);
        long beginTime = System.currentTimeMillis();
        //客户端请求超时的一个判断依据
        while (System.currentTimeMillis() - beginTime < 3*1000) {
            Object object = RESP_MAP.get(rpcInvocation.getUuid());
            if (object instanceof RpcInvocation) {
                return ((RpcInvocation)object).getResponse();
            }
        }
        throw new TimeoutException("client wait server's response timeout!");
    }
```
## 注册中心层
当服务提供者呈现集群模式的时候，客户端需要去获取服务提供者的诸多信息，服务提供者将自己的地址和接口等信息注册到注册中心，服务调用方只需要订阅注册中心即可

定义有`RegistryService`接口来实现服务的注册和下线
```java
/**
 * 服务注册，包括服务的注册，下线，订阅，取消订阅四个动作
 */
public interface RegistryService {
    /**
     * 注册URL，将服务写入注册中心节点
     * 当出现网络抖动的时候需要进行适当的重试做法
     * 注册服务url的时候需要写入持久化文件中
     *
     * @param url
     */
    void register(URL url);

    /**
     * 服务下线
     * 持久化节点是无法进行服务下线操作的
     * 下线的服务必须保证url是完整匹配的
     * 移除持久化文件中的一些内容信息
     *
     * @param url
     */
    void unRegister(URL url);

    /**
     * 订阅某个服务，通常是客户端在启动阶段需要调用的接口。
     * 客户端在启动过程中需要调用该函数，从注册中心中提取现有的服务提供者地址，从而实现服务订阅功能。
     *
     * @param url
     */
    void subscribe(URL url);


    /**
     * 取消订阅服务，当服务调用方不打算再继续订阅某些服务的时候，就需要调用该函数去取消服务的订阅功能，将注册中心的订阅记录进行移除操作。
     *
     * @param url
     */
    void doUnSubscribe(URL url);
}
```
其中，`URL`为封装的服务配置信息
```java

/**
 * URL配置总线类，将RPC的主要配置封装在其中
 */
public class URL {
    /**
     * 服务器应用名称
     */
    private String applicationName;
    /**
     * 注册到节点的服务名称
     */
    private String serviceName;
    /**
     * 自定义扩展，包括分组，权重，服务提供者地址和端口等
     */
    private Map<String, String> parameters = new HashMap<>();
}
```
为了监听服务提供节点的状态，使用观察者模式，设置监听事件和监听者接口，用于通知客户端节点的更新信息
```java
/**
 * 事件接口，用于装在需要传递的数据信息
 */
public interface RpcEvent {

    Object getData();

    RpcEvent setData(Object data);
}
```
```java
/**
 * 事件监听器
 *
 * @param <T>
 */
public interface RpcListener<T> {
    void callback(Object t);
}
```
并定义有监听器加载类，用于管理监听器和事件通知
```java
/**
 * 事件管理类，负责发送事件
 */
public class RpcListenerLoader {

    private static List<RpcListener> rpcListenerList = new ArrayList<>();

    private static ExecutorService eventThreadPool = Executors.newFixedThreadPool(2);

    public static void registerListener(RpcListener listener) {
        rpcListenerList.add(listener);
    }

    public void init() {
        registerListener(new ServiceUpdateListener());
    }

    /**
     * 获取接口上的泛型T
     *
     * @param o 接口
     */
    public static Class<?> getInterfaceT(Object o) {
        Type[] types = o.getClass().getGenericInterfaces();
        ParameterizedType parameterizedType = (ParameterizedType) types[0];
        Type type = parameterizedType.getActualTypeArguments()[0];
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        return null;
    }

    /**
     * 发送事件，同时触发监听
     *
     * @param rpcEvent
     */
    public static void sendEvent(RpcEvent rpcEvent) {
        if (CommonUtils.isEmptyList(rpcListenerList)) {
            return;
        }
        for (RpcListener<?> listener : rpcListenerList) {
            Class<?> type = getInterfaceT(listener);
            if (type.equals(rpcEvent.getData())) {
                eventThreadPool.execute(() -> {
                    try {
                        listener.callback(rpcEvent.getData());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
```

## 路由层
当目标服务众多的时候，使用路由层来确定请求的服务提供者

定义有`Router`接口用于进行路由选择，并给出了该接口的随机和轮询两种实现
```java
public interface Router {
    /**
     * 刷新路由数组
     *
     * @param selector
     */
    void refreshRouterArr(Selector selector);
    /**
     * 获取到请求到连接通道
     *
     * @return
     */
    ChannelFutureWrapper select(Selector selector);
    /**
     * 更新权重信息
     *
     * @param url
     */
    void updateWeight(URL url);
}
```
`Selector`封装了服务名和节点信息
```java
/**
 * 路由选择器类，封装有服务名称和服务对象的连接列表
 */
public class Selector {

    private String providerServiceName;

    private ChannelFutureWrapper[] channelFutureWrappers;
}
```
```java
/**
 * ChannelFuture的包装类
 */
public class ChannelFutureWrapper {
    /**
     * 与服务节点的连接
     **/
    private ChannelFuture channelFuture;

    private String host;

    private Integer port;

    private Integer weight;

    private String group;
}
```
调用：
```java
    /**
     * 获得与服务节点的连接
     *
     * @param rpcInvocation
     * @return
     */
    public static ChannelFuture getChannelFuture(RpcInvocation rpcInvocation) {
        ...
        Selector selector = new Selector();
        selector.setProviderServiceName(providerServiceName);
        selector.setChannelFutureWrappers(channelFutureWrappers);
        ChannelFuture channelFuture = ROUTER.select(selector).getChannelFuture();
        return channelFuture;
    }
```

## 协议层
在使用RPC框架进行远程调用的时候，对数据信息进行统一的包装和组织，再将其发送到目标机器中解析，需要进行各种序列化，反序列化和协议组装

协议被封装成类`RpcProtocol`，其结构如下：
```java
/**
 * 自定义协议
 */
public class RpcProtocol implements Serializable {
    private static final long serialVersionUID = 5359096060555795690L;
    /**
     * 魔法数，主要是在做服务通讯的时候定义的一个安全检测，确认当前请求的协议是否合法
     */
    private short magicNumber = MAGIC_NUMBER;
    /**
     * 协议传输核心数据的长度。
     * 当服务端的接收能力有限，可以对该字段进行赋值。
     * 当读取到的网络数据包中的contentLength字段已经超过预期值的话，就不会去读取content字段
     */
    private int contentLength;
    /**
     * 核心的传输数据，这里核心的传输数据主要是请求的服务名称，请求服务的方法名称，请求参数内容。
     * 为了方便后期扩展，这些核心的请求数据我都统一封装到了RpcInvocation对象当中
     */
    private byte[] content;
    
 }
```
其中，`content`是`RpcInvocation`序列化为字节流的信息，其结构如下：
```java
/**
 * 远程调用的方法信息，封装了请求方法的参数和返回值等信息
 */
public class RpcInvocation implements Serializable {

    private static final long serialVersionUID = 8447213193317732435L;
    /**
     * 请求的目标方法
     */
    private String targetMethod;
    /**
     * 请求的目标服务器
     */
    private String targetServiceName;
    /**
     * 请求参数信息
     */
    private Object[] args;
    /**
     * 用于匹配请求和响应的关键值，通过uuid来匹配对应的请求线程
     * 当请求从客户端发出的时候，会有一个uuid用于记录发出的请求，
     * 待数据返回的时候通过uuid来匹配对应的请求线程，并且返回给调用线程
     */
    private String uuid;
    /**
     * 接口响应的数据，如果为void则为null
     */
    private Object response;
    /**
     * 服务端传来的错误信息
     **/
    private Throwable e;
    /**
     * 重试次数
     **/
    private int retry;

    private Map<String, Object> attachments = new HashMap<>();
```

## 序列化层
定义有`SerializeFactory`接口来支持多种序列化方法
```java
/**
 * 序列化接口，为了兼容各种序列化方法
 */
public interface SerializeFactory {
    /**
     * 序列化
     *
     * @param t
     * @param <T>
     * @return
     */
    <T> byte[] serialize(T t);

    /**
     * 反序列化
     *
     * @param data
     * @param clazz
     * @param <T>
     * @return
     */
    <T> T deserialize(byte[] data, Class<T> clazz);
}
```
使用JDK，Hessian，Kryo和FastJson的方式实现该接口，实现多种序列化和反序列化方案\
使用处替换为：
```java
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 服务端接收数据统一以RPCProtocol协议的格式接收
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(), RpcInvocation.class);
        ...
    }
```
## 拓展组件
在代理后面使用责任链模式，对服务进行扩展

使用责任链设计模式的好处：

- 发送者与接收方的处理对象类之间解耦；
- 封装每个处理对象，处理类的最小封装原则；
- 可以任意添加处理对象，调整处理对象之间的顺序，提高了维护性和可拓展性，可以根据需求新增处理类，满足开闭原则；
- 增强了对象职责指派的灵活性，当流程发生变化的时候，可以动态地改变链内的调动次序可动态的新增或者删除；
- 责任链简化了对象之间的连接。每个对象只需保持一个指向其后继者的引用，不需保持其他所有处理者的引用，这避免了使用众多的`if···else`语句；
- 责任分担。每个类只需要处理自己该处理的工作，不该处理的传递给下一个对象完成，明确各类的责任范围，符合类的单一职责原则。

定义责任链的过滤器接口和责任链的执行类
```java
/**
 * 过滤器接口
 */
public interface Filter {
}
```
```java
/**
 * 服务端过滤器
 */
public interface ServerFilter extends Filter {

    /**
     * 执行核心过滤逻辑
     *
     * @param rpcInvocation
     */
    void doFilter(RpcInvocation rpcInvocation);
}
```
```java
/**
 * 服务端模块的过滤器链路类
 */
public class ServerFilterChain {
    private static List<ServerFilter> serverFilterList = new ArrayList<>();

    public void addServerFilter(ServerFilter serverFilter) {
        serverFilterList.add(serverFilter);
    }

    public void doFilter(RpcInvocation rpcInvocation) {
        for (ServerFilter filter : serverFilterList) {
            filter.doFilter(rpcInvocation);
        }
    }
}
```
```java
/**
 * 客户端过滤器
 */
public interface ClientFilter extends  Filter {
    /**
     * 执行过滤链
     *
     * @param src
     * @param rpcInvocation
     * @return
     */
    void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation);
}
```
```java
/**
 * 客户端模块的过滤器链路类
 */
public class ClientFilterChain {
    private static List<ClientFilter> clientFilterList = new ArrayList<>();

    public void addClientFilter(ClientFilter clientFilter) {
        clientFilterList.add(clientFilter);
    }

    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        for (ClientFilter clientFilter : clientFilterList) {
            clientFilter.doFilter(src, rpcInvocation);
        }
    }
}
```

## 容错层
设计思路为：将服务端的异常信息统一采集起来，返回给到调用方并且将堆栈记录打印。
在`RpcInvocation`中，封装有属性`Throwable e`，用于存储服务端传来的错误信息，在服务端的核心代码中捕获异常并封装在其中
```java
// 寻找得到对应的并执行
for (Method method : methods) {
    try {
        if (method.getName().equals(rpcInvocation.getTargetMethod())) {
            if (method.getReturnType().equals(Void.TYPE)) {
                method.invoke(aimObject, rpcInvocation.getArgs());
            } else {
                result = method.invoke(aimObject, rpcInvocation.getArgs());
            }
            break;
        }
    } catch (Exception e) {
        // 业务异常
        rpcInvocation.setE(e);
    }
    // 设置response
    rpcInvocation.setResponse(result);
    // 后置过滤器
    SERVER_AFTER_FILTER_CHAIN.doFilter(rpcInvocation);
    // 再次封装为RpcProtocol返回给客户端
    RpcProtocol respRpcProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInvocation));
    serverChannelReadData.getChannelHandlerContext().writeAndFlush(respRpcProtocol);
}
```

对于数据包体积过大导致初次传输Netty报错的问题，在协议尾部加入分隔符，且通过参数定义每次传输的最大数据包体积

在编码器中加入了相应代码
```java
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcProtocol msg, ByteBuf out) throws Exception {
        out.writeShort(msg.getMagicNumber());
        out.writeInt(msg.getContentLength());
        out.writeBytes(msg.getContent());
        // 数据包过大导致netty拆分成多个包时出现的异常
        out.writeBytes(DEFAULT_DECODE_CHAR.getBytes());
    }
```
```java
    bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            ByteBuf delimiter = Unpooled.copiedBuffer(DEFAULT_DECODE_CHAR.getBytes());
            socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(serverConfig.getMaxServerRequestData(), delimiter));
            socketChannel.pipeline().addLast(new RpcEncoder());
            socketChannel.pipeline().addLast(new RpcDecoder());
            socketChannel.pipeline().addLast(new ServerHandler());
        }
    });
```

### 超时重试机制
- 目标集群中有A，B服务器，A服务器性能不佳，处理请求比较缓慢，B服务器性能优于A，所以当接口调用A出现超时之后，可以尝试重新发起调用，将请求转到B上从而获取数据结果。
- 网络因为某些特殊异常，导致突然间断，此时可以通过重试机制发起二次调用，这时候重试机制就对接口的整体可用性有了一定的保障。
- 对于一些对数据重复性较为敏感的接口，例如转账，下单，以及一些和金融相关的接口，当接口调用出现超时之后，并不好确认数据包是否已经抵达到目标服务，所以这类场景下对接口设置超时重试功能需要有所斟酌。

在`RpcInvocation`的参数中加入有`retry`属性，用于存储可以进行重试的次数，如果不使用重试机制，则设置为0
```java
    if (OBJECT.equals(object)) {
        //超时重试
        if (System.currentTimeMillis() - beginTime > timeOut) {
            retryTimes++;
            //重新请求
            rpcInvocation.setResponse(null);
            //每次重试之后都会将retry值扣减1
            rpcInvocation.setRetry(rpcInvocation.getRetry() - 1);
            RESP_MAP.put(rpcInvocation.getUuid(), OBJECT);
            SEND_QUEUE.add(rpcInvocation);
        }
    }
```
![](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4095251cf43546f99a0bd0dc694ff9ae~tplv-k3u1fbpfcp-jj-mark:1512:0:0:0:q75.awebp)

### 服务端保护机制
对服务端的保护机制包括控制业务应用整体的连接上限和单个服务请求的上限设置

对于整体的连接上限，使用一个`MaxConnectionLimitHandler`来设置连接上限
```java
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("connect limit handler");
        Channel channel = (Channel) msg;
        int conn = numConnection.incrementAndGet();
        if (conn > 0 && conn <= maxConnectionNum) {
            this.childChannel.add(channel);
            channel.closeFuture().addListener(future -> {
                childChannel.remove(channel);
                numConnection.decrementAndGet();
            });
        } else {
            numConnection.decrementAndGet();
            // 避免产生大量的time_wait连接
            channel.config().setOption(ChannelOption.SO_LINGER, 0);
            channel.unsafe().closeForcibly();
            numDroppedConnections.increment();
            //这里加入一道cas可以减少一些并发请求的压力,定期地执行一些日志打印
            if (loggingScheduled.compareAndSet(false, true)) {
                ctx.executor().schedule(this::writeNumDroppedConnectionLog, 1, TimeUnit.SECONDS);
            }
        }
    }
```
```java
// 连接防护的handler应该绑定在main-reactor上
bootstrap.handler(new MaxConnectionLimitHandler(serverConfig.getMaxConnections()))
```

对于当个服务请求上限，使用信号量的方式，编写对应的过滤器来实现
```java
public class ServerServiceBeforeLimitFilterImpl implements ServerFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerServiceBeforeLimitFilterImpl.class);

    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String serviceName = rpcInvocation.getTargetServiceName();
        ServerServiceSemaphoreWrapper serverServiceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName);
        // 从缓存中提取semaphore对象
        Semaphore semaphore = serverServiceSemaphoreWrapper.getSemaphore();
        boolean tryResult = semaphore.tryAcquire();
        if (!tryResult) {
            LOGGER.error("[ServerServiceBeforeLimitFilterImpl] {}'s max request is {},reject now",
                    rpcInvocation.getTargetServiceName(), serverServiceSemaphoreWrapper.getMaxNums());
            MaxServiceLimitRequestException iRpcException = new MaxServiceLimitRequestException(rpcInvocation);
            rpcInvocation.setE(iRpcException);
            throw iRpcException;
        }
    }
}
```
其中，`ServerServiceSemaphoreWrapper`封装有各个服务的连接上限信息
```java
/**
 * 封装有server单个服务的连接上限信息
 */
public class ServerServiceSemaphoreWrapper {
    /**
     * 信号量，记录当前；连接数
     **/
    private Semaphore semaphore;
    /**
     * 最大连接数
     **/
    private int maxNums;
}
```

## 接入层
涉及如何接入Spring框架，编写sprint-boot-starter

编写两个注解：
```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcReference {

    String url() default "";

    String group() default "default";

    String serviceToken() default "";

    int timeOut() default 3000;

    int retry() default 1;

    boolean async() default false;
}
```
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RpcService {

    int limit() default 0;

    String group() default "default";

    String serviceToken() default "";
}
```
`RpcService`标有`@Component`，即被这两个注解标记了的类会被注入spring容器中

然后是服务端和客户端的自动配置类
```java
/**
 * rpc服务端的自动装配类
 */
public class RpcServerAutoConfiguration implements InitializingBean, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerAutoConfiguration.class);

    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        Server server = null;
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (beanMap.size() == 0) {
            //说明当前应用内部不需要对外暴露服务，无需执行下边多余的逻辑
            return;
        }
        long begin = System.currentTimeMillis();
        server = new Server();
        RpcListenerLoader rpcListenerLoader = new RpcListenerLoader();
        rpcListenerLoader.init();
        for (String beanName : beanMap.keySet()) {
            Object bean = beanMap.get(beanName);
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            ServiceWrapper dataServiceWrapper = new ServiceWrapper(bean, rpcService.group());
            dataServiceWrapper.setServiceToken(rpcService.serviceToken());
            dataServiceWrapper.setLimit(rpcService.limit());
            LOGGER.info(">>>>>>>>>>>>>>> [rpc] {} export success! >>>>>>>>>>>>>>> ",beanName);
        }    long end = System.currentTimeMillis();
        ApplicationShutdownHook.registryShutdownHook();
        server.startApplication();
        LOGGER.info(" ================== [{}] started success in {}s ================== ",server.getServerConfig().getApplicationName(),((double)end-(double)begin)/1000);

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
```
```java
/**
 * rpc客户端的自动装配类
 */
public class RpcClientAutoConfiguration implements BeanPostProcessor, ApplicationListener<ApplicationReadyEvent> {

    private static cn.bobasyu.core.client.RpcReference rpcReference = null;
    private static Client client = null;
    private volatile boolean needInitClient = false;
    private volatile boolean hasInitClientConfig = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientAutoConfiguration.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(cn.bobasyu.framework.spring.starter.common.RpcReference.class)) {
                if (!hasInitClientConfig) {
                    //初始化客户端的配置
                    client = new Client();
                    try {
                        rpcReference = client.initClientApplication();
                    } catch (Exception e) {
                        LOGGER.error("[IRpcClientAutoConfiguration] postProcessAfterInitialization has error ", e);
                        throw new RuntimeException(e);
                    }
                    hasInitClientConfig = true;
                }
                needInitClient = true;
                RpcReference rpcReferenceEnum = field.getAnnotation(RpcReference.class);
                try {
                    field.setAccessible(true);
                    Object refObj = field.get(bean);
                    RpcReferenceWrapper rpcReferenceWrapper = new RpcReferenceWrapper();
                    rpcReferenceWrapper.setAimClass(field.getType());
                    rpcReferenceWrapper.setGroup(rpcReferenceEnum.group());
                    rpcReferenceWrapper.setServiceToken(rpcReferenceEnum.serviceToken());
                    rpcReferenceWrapper.setUrl(rpcReferenceEnum.url());
                    rpcReferenceWrapper.setTimeOut(rpcReferenceEnum.timeOut());
                    //失败重试次数
                    rpcReferenceWrapper.setRetry(rpcReferenceEnum.retry());
                    rpcReferenceWrapper.setAsync(rpcReferenceEnum.async());
                    refObj = rpcReference.get(rpcReferenceWrapper);
                    field.set(bean, refObj);
                    client.doSubscribeService(field.getType());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
        return bean;
    }


    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        if (needInitClient && client != null) {
            LOGGER.info(" ================== [{}] started success ================== ", client.getClientConfig().getApplicationName());
            ConnectionHandler.setBootstrap(client.getBootstrap());
            client.doConnectServer();
            client.startClient();
        }
    }
}
```
在使用时，服务端的服务类加上`@IRpcService`注解来暴露RPC服务，而客户端使用服务的类中的属性加入`@IRpcReference`注解即可完成依赖注入
