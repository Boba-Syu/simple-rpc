package cn.bobasyu.core.common.cache;

import cn.bobasyu.core.common.ChannelFuturePollingRef;
import cn.bobasyu.core.common.ChannelFutureWrapper;
import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.config.ClientConfig;
import cn.bobasyu.core.filter.client.ClientFilterChain;
import cn.bobasyu.core.registry.URL;
import cn.bobasyu.core.router.Router;
import cn.bobasyu.core.serialize.SerializeFactory;
import cn.bobasyu.core.spi.ExtensionLoader;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class CommonClientCache {

    public static BlockingQueue<RpcInvocation> SEND_QUEUE = new ArrayBlockingQueue(100);

    public static Map<String, Object> RESP_MAP = new ConcurrentHashMap<>();

    public static ClientConfig CLIENT_CONFIG;

    /**
     * provider名称，该服务有哪些集群URL
     */
    public static List<String> SUBSCRIBE_SERVICE_LIST = new ArrayList<>();

    public static Map<String, List<URL>> URL_MAP = new ConcurrentHashMap<>();

    public static Set<String> SERVER_ADDRESS = new HashSet<>();

    /**
     * 每次进行远程调用的时候都是从这里面去选择服务提供者
     */
    public static Map<String, List<ChannelFutureWrapper>> CONNECT_MAP = new ConcurrentHashMap<>();

    public static Map<String, ChannelFutureWrapper[]> SERVICE_ROUTER_MAP = new ConcurrentHashMap<>();

    public static ChannelFuturePollingRef CHANNEL_FUTURE_POLLING_REF = new ChannelFuturePollingRef();

    public static Router ROUTER;

    public static SerializeFactory CLIENT_SERIALIZE_FACTORY;

    public static ClientFilterChain CLIENT_FILTER_CHAIN;

    public static ExtensionLoader EXTENSION_LOADER = new ExtensionLoader();

}
