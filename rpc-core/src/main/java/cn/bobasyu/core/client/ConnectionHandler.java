package cn.bobasyu.core.client;

import cn.bobasyu.core.common.ChannelFutureWrapper;
import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.router.Selector;
import cn.bobasyu.core.utils.CommonUtils;
import com.esotericsoftware.minlog.Log;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static cn.bobasyu.core.common.cache.CommonClientCache.*;

/**
 * URL处理类，当注册中心的节点新增或者移除或者权重变化的时候，这个类主要负责对内存中的url做变更
 * 封装了连接的建立，断开等方法
 */
public class ConnectionHandler {
    /**
     * 核心的连接处理器
     * 专门用于负责和服务端构建连接通信
     */
    private static Bootstrap bootstrap;

    public static void setBootstrap(Bootstrap bootstrap) {
        ConnectionHandler.bootstrap = bootstrap;
    }

    /**
     * 构建单个连接通道 元操作，既要处理连接，还要统一将连接进行内存存储管理
     *
     * @param providerIp
     * @return
     * @throws InterruptedException
     */
    public static void connect(String providerServiceName, String providerIp) throws InterruptedException {
        if (bootstrap == null) {
            throw new RuntimeException("bootstrap can not be null");
        }
        //格式错误类型的信息
        if (!providerIp.contains(":")) {
            return;
        }
        String[] providerAddress = providerIp.split(":");
        String ip = providerAddress[0];
        Integer port = Integer.parseInt(providerAddress[1]);

        ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
        ChannelFutureWrapper channelFutureWrapper = new ChannelFutureWrapper();
        channelFutureWrapper.setChannelFuture(channelFuture);
        channelFutureWrapper.setHost(ip);
        channelFutureWrapper.setPort(port);
        SERVER_ADDRESS.add(providerIp);
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
        if (CommonUtils.isEmptyList(channelFutureWrappers)) {
            channelFutureWrappers = new ArrayList<>();
        }
        channelFutureWrappers.add(channelFutureWrapper);
        CONNECT_MAP.put(providerServiceName, channelFutureWrappers);
        Selector selector = new Selector();
        selector.setProviderServiceName(providerServiceName);
        ROUTER.refreshRouterArr(selector);
    }

    /**
     * 构建ChannelFuture
     *
     * @param ip
     * @param port
     * @return
     * @throws InterruptedException
     */
    public static ChannelFuture createChannelFuture(String ip, Integer port) throws InterruptedException {
        ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
        return channelFuture;
    }

    /**
     * 断开连接
     *
     * @param providerServiceName
     * @param providerIp
     */
    public static void disConnect(String providerServiceName, String providerIp) {
        SERVER_ADDRESS.remove(providerIp);
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
        if (CommonUtils.isNotEmptyList(channelFutureWrappers)) {
            Iterator<ChannelFutureWrapper> iterator = channelFutureWrappers.iterator();
            while (iterator.hasNext()) {
                ChannelFutureWrapper channelFutureWrapper = iterator.next();
                if (providerIp.equals(channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort())) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 获得与服务节点的连接
     *
     * @param rpcInvocation
     * @return
     */
    public static ChannelFuture getChannelFuture(RpcInvocation rpcInvocation) {
        String providerServiceName = rpcInvocation.getTargetServiceName();
        ChannelFutureWrapper[] channelFutureWrappers = SERVICE_ROUTER_MAP.get(providerServiceName);
        if (channelFutureWrappers == null || channelFutureWrappers.length == 0) {
            rpcInvocation.setRetry(0);
            rpcInvocation.setE(new RuntimeException("no provider exist for " + providerServiceName));
            rpcInvocation.setResponse(null);
            //直接交给响应线程那边处理（响应线程在代理类内部的invoke函数中，那边会取出对应的uuid的值，然后判断）
            RESP_MAP.put(rpcInvocation.getUuid(), rpcInvocation);
            Log.error("channelFutureWrapper is null");
            return null;
        }
        List<ChannelFutureWrapper> channelFutureWrappersList = new ArrayList<>(channelFutureWrappers.length);
        for (int i = 0; i < channelFutureWrappers.length; i++) {
            channelFutureWrappersList.add(channelFutureWrappers[i]);
        }
        // 责任链调用
        CLIENT_FILTER_CHAIN.doFilter(channelFutureWrappersList, rpcInvocation);
        Selector selector = new Selector();
        selector.setProviderServiceName(providerServiceName);
        selector.setChannelFutureWrappers(channelFutureWrappers);
        ChannelFuture channelFuture = ROUTER.select(selector).getChannelFuture();
        return channelFuture;
    }

}
