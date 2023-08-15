package cn.bobasyu.core.filter.client;

import cn.bobasyu.core.common.ChannelFutureWrapper;
import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.filter.ClientFilter;

import java.util.ArrayList;
import java.util.List;
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
