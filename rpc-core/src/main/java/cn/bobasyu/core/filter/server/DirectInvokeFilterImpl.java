package cn.bobasyu.core.filter.server;

import cn.bobasyu.core.common.ChannelFutureWrapper;
import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.filter.ClientFilter;
import cn.bobasyu.core.utils.CommonUtils;

import java.util.Iterator;
import java.util.List;

/**
 * ip直连过滤器
 */
public class DirectInvokeFilterImpl implements ClientFilter {
    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        String url = (String) rpcInvocation.getAttachments().get("url");
        if (CommonUtils.isEmpty(url)) {
            return;
        }
        Iterator<ChannelFutureWrapper> channelFutureWrapperIterator = src.iterator();
        while (channelFutureWrapperIterator.hasNext()) {
            ChannelFutureWrapper channelFutureWrapper = channelFutureWrapperIterator.next();
            if (!(channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort()).equals(url)) {
                channelFutureWrapperIterator.remove();
            }
        }
        if (CommonUtils.isEmptyList(src)) {
            throw new RuntimeException("no match provider url for " + url);
        }
    }
}
