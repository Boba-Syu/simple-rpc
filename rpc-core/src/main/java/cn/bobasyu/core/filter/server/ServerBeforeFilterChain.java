package cn.bobasyu.core.filter.server;

import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.filter.ServerFilter;

import java.util.ArrayList;
import java.util.List;

public class ServerBeforeFilterChain {

    private static List<ServerFilter> serverFilters = new ArrayList<>();

    public void addServerFilter(ServerFilter iServerFilter) {
        serverFilters.add(iServerFilter);
    }

    public void doFilter(RpcInvocation rpcInvocation) {
        for (ServerFilter iServerFilter : serverFilters) {
            iServerFilter.doFilter(rpcInvocation);
        }
    }
}