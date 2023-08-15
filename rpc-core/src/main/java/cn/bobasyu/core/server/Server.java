package cn.bobasyu.core.server;

import cn.bobasyu.core.common.RpcDecoder;
import cn.bobasyu.core.common.RpcEncoder;
import cn.bobasyu.core.config.PropertiesBootstrap;
import cn.bobasyu.core.config.ServerConfig;
import cn.bobasyu.core.registry.RegistryService;
import cn.bobasyu.core.registry.URL;
import cn.bobasyu.core.utils.CommonUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import static cn.bobasyu.core.common.cache.CommonServerCache.PROVIDER_CLASS_MAP;
import static cn.bobasyu.core.common.cache.CommonServerCache.PROVIDER_URL_SET;

/**
 * 服务端，使用Netty实现
 */
public class Server {

    private static EventLoopGroup bossGroup = null;

    private static EventLoopGroup workerGroup = null;

    private ServerConfig serverConfig;

    private RegistryService registryService;

    public void initServerConfig() {
        ServerConfig serverConfig = PropertiesBootstrap.loadServerConfigFromLocal();
        this.setServerConfig(serverConfig);
    }

    public void startApplication() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_SNDBUF, 16 * 1024)
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        System.out.println("初始化provider过程");
                        socketChannel.pipeline().addLast(new RpcEncoder());
                        socketChannel.pipeline().addLast(new RpcDecoder());
                        socketChannel.pipeline().addLast(new ServerHandler());
                    }
                });
        this.batchExportUrl();
        bootstrap.bind(this.serverConfig.getServerPort());
    }

    /**
     * 将服务端的具体服务都暴露到注册中心，方便客户端进行调用
     */
    private void batchExportUrl() {
        Thread task = new Thread(() -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (URL url : PROVIDER_URL_SET) {
                this.registryService.register(url);
            }
        });
        task.start();
    }

    /**
     * 暴露服务信息
     *
     * @param serviceBean
     */
    public void exportService(Object serviceBean) {
        if (serviceBean.getClass().getInterfaces().length == 0) {
            throw new RuntimeException("service must had interfaces!");
        }
        Class<?>[] classes = serviceBean.getClass().getInterfaces();
        if (classes.length > 1) {
            throw new RuntimeException("service must only had one interfaces!");
        }
        // 默认选择该对象的第一个实现接口
        Class<?> interfaceClass = classes[0];
        // 需要注册的对象统一放在一个MAP集合中进行管理
        PROVIDER_CLASS_MAP.put(interfaceClass.getName(), serviceBean);
        URL url = new URL();
        url.setServiceName(interfaceClass.getName());
        url.setApplicationName(this.serverConfig.getApplicationName());
        url.addParameter("host", CommonUtils.getIpAddress());
        url.addParameter("port", String.valueOf(this.serverConfig.getServerPort()));
        PROVIDER_URL_SET.add(url);
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }
}