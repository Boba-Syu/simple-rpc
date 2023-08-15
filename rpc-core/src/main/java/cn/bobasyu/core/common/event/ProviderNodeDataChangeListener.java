package cn.bobasyu.core.common.event;

import cn.bobasyu.core.common.ChannelFutureWrapper;
import cn.bobasyu.core.registry.URL;
import cn.bobasyu.core.registry.zookeeper.ProviderNodeInfo;

import java.util.List;

import static cn.bobasyu.core.common.cache.CommonClientCache.CONNECT_MAP;
import static cn.bobasyu.core.common.cache.CommonClientCache.ROUTER;

/**
 * RpcNodeChangeEvent事件的监听器
 */
public class ProviderNodeDataChangeListener implements RpcListener<RpcNodeChangeEvent> {
    @Override
    public void callback(Object t) {
        ProviderNodeInfo providerNodeInfo = (ProviderNodeInfo) t;
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerNodeInfo.getServiceName());
        for (ChannelFutureWrapper channelFutureWrapper : channelFutureWrappers) {
            String address = channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort();
            if (address.equals(providerNodeInfo.getAddress())) {
                // 修改权重
                channelFutureWrapper.setWeight(providerNodeInfo.getWeight());
                URL url = new URL();
                url.setServiceName(providerNodeInfo.getServiceName());
                // 更新权重
                ROUTER.updateWeight(url);
                break;
            }
        }
    }
}
