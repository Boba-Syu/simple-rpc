package cn.bobasyu.core.filter;

import cn.bobasyu.core.common.ChannelFutureWrapper;
import cn.bobasyu.core.common.RpcInvocation;

import java.util.List;

/**
 * 客户端过滤器
 */
public interface ClientFilter extends  Filter {
    /**
     * 执行过滤链
     *
     * @param src
     * @param rpcInvocation
     * @return
     */
    void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation);
}
