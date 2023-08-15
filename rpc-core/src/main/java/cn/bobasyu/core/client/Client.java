package cn.bobasyu.core.client;

import cn.bobasyu.core.common.RpcDecoder;
import cn.bobasyu.core.common.RpcEncoder;
import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.common.RpcProtocol;
import cn.bobasyu.core.common.event.RpcListenerLoader;
import cn.bobasyu.core.config.ClientConfig;
import cn.bobasyu.core.config.PropertiesBootstrap;
import cn.bobasyu.core.proxy.javassist.JavassistProxyFactory;
import cn.bobasyu.core.proxy.jdk.JDKProxyFactory;
import cn.bobasyu.core.registry.URL;
import cn.bobasyu.core.registry.zookeeper.AbstractRegister;
import cn.bobasyu.core.registry.zookeeper.AbstractZookeeperClient;
import cn.bobasyu.core.registry.zookeeper.ZookeeperRegister;
import cn.bobasyu.core.router.RandomRouterImpl;
import cn.bobasyu.core.router.RotateRouterImpl;
import cn.bobasyu.core.serialize.fastjson.FastJsonSerializeFactory;
import cn.bobasyu.core.serialize.hessian.HessianSerializeFactory;
import cn.bobasyu.core.serialize.jdk.JdkSerializeFactory;
import cn.bobasyu.core.serialize.kryo.KryoSerializeFactory;
import cn.bobasyu.core.utils.CommonUtils;
import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static cn.bobasyu.core.common.RpcConstants.*;
import static cn.bobasyu.core.common.cache.CommonClientCache.*;
import static cn.bobasyu.core.common.cache.CommonServerCache.SEND_QUEUE;

/**
 * 客户端，使用Netty实现
 * 通过代理工厂获取调用对象的代理对象，然后通过代理对象将数据让如发送队列，最后通过异步线程将发送队列内部的数据一个个发送到服务段，并且等待服务端响应对应的数据结果
 */
public class Client {

    private Logger logger = LoggerFactory.getLogger(Client.class);

    public static EventLoopGroup clientGroup = new NioEventLoopGroup();

    private ClientConfig clientConfig;

    private AbstractRegister abstractRegister;

    private RpcListenerLoader rpcListenerLoader;

    private Bootstrap bootstrap = new Bootstrap();

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public RpcReference initClientApplication() {
        this.bootstrap.group(this.clientGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(new RpcEncoder());
                socketChannel.pipeline().addLast(new RpcDecoder());
                socketChannel.pipeline().addLast(new ClientHandler());
            }
        });

        this.rpcListenerLoader = new RpcListenerLoader();
        this.rpcListenerLoader.init();
        this.clientConfig = PropertiesBootstrap.loadClientConfigFromLocal();

        if ("javassist".equals(this.clientConfig.getProxyType())) {
            return new RpcReference(new JavassistProxyFactory());
        } else {
            return new RpcReference(new JDKProxyFactory());
        }
    }

    /**
     * 启动服务之前需要预先订阅对应的dubbo服务
     *
     * @param serviceBean
     */
    public void doSubscribeService(Class<?> serviceBean) {
        if (this.abstractRegister == null) {
            this.abstractRegister = new ZookeeperRegister(this.clientConfig.getRegisterAddr());
        }
        URL url = new URL();
        url.setApplicationName(this.clientConfig.getApplicationName());
        url.setServiceName(serviceBean.getName());
        url.addParameter("host", CommonUtils.getIpAddress());
        this.abstractRegister.subscribe(url);
    }

    /**
     * 开始和各个provider建立连接
     */
    public void doConnectServer() {
        for (String providerServiceName : SUBSCRIBE_SERVICE_LIST) {
            List<String> providerIps = this.abstractRegister.getProviderIps(providerServiceName);
            for (String providerIp : providerIps) {
                try {
                    ConnectionHandler.connect(providerServiceName, providerIp);
                } catch (InterruptedException e) {
                    this.logger.error("[doConnectServer] connect fail ", e);
                }
            }
            URL url = new URL();
            url.setServiceName(providerServiceName);
            url.addParameter("servicePath", providerServiceName + "/provider");
            url.addParameter("providerIps", JSON.toJSONString(providerIps));
            // 客户端在此新增一个订阅功能
            this.abstractRegister.doAfterSubscribe(url);
        }
    }

    /**
     * 开启发送线程，专门从事将数据包发送给服务端
     *
     * @param channelFuture
     */
    private void startClient(ChannelFuture channelFuture) {
        Thread asyncSendJob = new Thread(new AsyncSendJob(channelFuture));
        asyncSendJob.start();
    }

    /**
     * 异步发送信息任务
     */
    static class AsyncSendJob implements Runnable {

        private ChannelFuture channelFuture;

        public AsyncSendJob(ChannelFuture channelFuture) {
            this.channelFuture = channelFuture;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // 阻塞模式
                    RpcInvocation data = SEND_QUEUE.take();
                    // 将RpcInvocation封装到RpcProtocol中
                    String json = JSON.toJSONString(data);
                    RpcProtocol rpcProtocol = new RpcProtocol(json.getBytes());

                    //netty的通道负责发送数据给服务端
                    channelFuture.channel().writeAndFlush(rpcProtocol);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化路由策略
     */
    private void initClientConfig() {
        // 初始化路由策略
        String routerStrategy = clientConfig.getRouterStrategy();
        switch (routerStrategy) {
            case RANDOM_ROUTER_TYPE:
                ROUTER = new RandomRouterImpl();
                break;
            case ROTATE_ROUTER_TYPE:
                ROUTER = new RotateRouterImpl();
                break;
            default:
                throw new RuntimeException("no match router strategy for" + routerStrategy);
        }
        // 初始化序列化策略
        String serializeStrategy = clientConfig.getRouterStrategy();
        switch (serializeStrategy) {
            case JDK_SERIALIZE_TYPE:
                CLIENT_SERIALIZE_FACTORY = new JdkSerializeFactory();
                break;
            case FAST_JSON_SERIALIZE_TYPE:
                CLIENT_SERIALIZE_FACTORY = new FastJsonSerializeFactory();
                break;
            case HESSIAN2_SERIALIZE_TYPE:
                CLIENT_SERIALIZE_FACTORY = new HessianSerializeFactory();
                break;
            case KRYO_SERIALIZE_TYPE:
                CLIENT_SERIALIZE_FACTORY = new KryoSerializeFactory();
                break;
            default:
                throw new RuntimeException("no match serialize strategy for" + serializeStrategy);
        }
    }
}
