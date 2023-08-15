package cn.bobasyu.core.filter;

import cn.bobasyu.core.common.RpcInvocation;

/**
 * 服务端过滤器
 */
public interface ServerFilter extends Filter {

    /**
     * 执行核心过滤逻辑
     *
     * @param rpcInvocation
     */
    void doFilter(RpcInvocation rpcInvocation);
}