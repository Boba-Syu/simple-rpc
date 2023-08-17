package cn.bobasyu.core.filter.server;

import cn.bobasyu.core.annotation.SPI;
import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.common.ServerServiceSemaphoreWrapper;
import cn.bobasyu.core.filter.ServerFilter;

import static cn.bobasyu.core.common.cache.CommonServerCache.SERVER_SERVICE_SEMAPHORE_MAP;

/**
 * 限流的后置操作，释放
 */
@SPI("after")
public class ServerServiceAfterLimitFilterImpl implements ServerFilter {
    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String serviceName = rpcInvocation.getTargetServiceName();
        ServerServiceSemaphoreWrapper serverServiceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName);
        serverServiceSemaphoreWrapper.getSemaphore().release();
    }
}
