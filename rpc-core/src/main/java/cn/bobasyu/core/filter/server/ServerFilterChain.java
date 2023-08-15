package cn.bobasyu.core.filter.server;

import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.filter.ServerFilter;

import java.util.ArrayList;
import java.util.List;

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
