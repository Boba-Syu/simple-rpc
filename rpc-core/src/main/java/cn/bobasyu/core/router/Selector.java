package cn.bobasyu.core.router;

import cn.bobasyu.core.common.ChannelFutureWrapper;

/**
 * 路由选择器类，封装有服务名称和服务对象的连接列表
 */
public class Selector {

    private String providerServiceName;

    private ChannelFutureWrapper[] channelFutureWrappers;

    public String getProviderServiceName() {
        return providerServiceName;
    }

    public void setProviderServiceName(String providerServiceName) {
        this.providerServiceName = providerServiceName;
    }

    public ChannelFutureWrapper[] getChannelFutureWrappers() {
        return channelFutureWrappers;
    }

    public void setChannelFutureWrappers(ChannelFutureWrapper[] channelFutureWrappers) {
        this.channelFutureWrappers = channelFutureWrappers;
    }
}
