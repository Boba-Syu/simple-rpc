package cn.bobasyu.core.common.event;

import cn.bobasyu.core.client.ConnectionHandler;
import cn.bobasyu.core.common.ChannelFutureWrapper;
import cn.bobasyu.core.common.event.data.URLChangeWrapper;
import cn.bobasyu.core.utils.CommonUtils;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.bobasyu.core.common.cache.CommonClientCache.CONNECT_MAP;


public class ServiceUpdateListener implements RpcListener<RpcUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUpdateListener.class);

    @Override
    public void callback(Object t) {
        // 获取到子节点的数据信息
        URLChangeWrapper urlChangeWrapper = (URLChangeWrapper) t;
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(urlChangeWrapper.getServiceName());
        if (CommonUtils.isEmptyList(channelFutureWrappers)) {
            LOGGER.error("[ServiceUpdateListener] channelFutureWrappers is empty");
        } else {
            List<String> matchProviderUrl = urlChangeWrapper.getProviderUrl();
            Set<String> finalUrl = new HashSet<>();
            List<ChannelFutureWrapper> finalChannelFutureWrappers = new ArrayList<>();
            for (ChannelFutureWrapper channelFutureWrapper : channelFutureWrappers) {
                String oldServerAddress = channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort();
                // 如果没有老的URL，说明已经被移除了
                if (!matchProviderUrl.contains(oldServerAddress)) {
                    continue;
                }
                finalChannelFutureWrappers.add(channelFutureWrapper);
                finalUrl.add(oldServerAddress);
            }
            // 此时老的URL已经被移除了，开始检查是否有新的URL
            List<ChannelFutureWrapper> newChannelFutureWrapper = new ArrayList<>();
            for (String newProviderUrl : matchProviderUrl) {
                if (!finalUrl.contains(newProviderUrl)) {
                    ChannelFutureWrapper channelFutureWrapper = new ChannelFutureWrapper();
                    String host = newProviderUrl.split(":")[0];
                    Integer port = Integer.valueOf(newProviderUrl.split(":")[1]);
                    channelFutureWrapper.setHost(host);
                    channelFutureWrapper.setPort(port);
                    ChannelFuture channelFuture;
                    try {
                        channelFuture = ConnectionHandler.createChannelFuture(host, port);
                        channelFutureWrapper.setChannelFuture(channelFuture);
                        newChannelFutureWrapper.add(channelFutureWrapper);
                        finalUrl.add(newProviderUrl);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            finalChannelFutureWrappers.addAll(newChannelFutureWrapper);
            // 更新服务
            CONNECT_MAP.put(urlChangeWrapper.getServiceName(), finalChannelFutureWrappers);
        }
    }

}
