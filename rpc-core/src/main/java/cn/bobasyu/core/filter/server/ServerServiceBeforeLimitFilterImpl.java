package cn.bobasyu.core.filter.server;

import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.common.ServerServiceSemaphoreWrapper;
import cn.bobasyu.core.exception.MaxServiceLimitRequestException;
import cn.bobasyu.core.filter.ServerFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

import static cn.bobasyu.core.common.cache.CommonServerCache.SERVER_SERVICE_SEMAPHORE_MAP;

public class ServerServiceBeforeLimitFilterImpl implements ServerFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerServiceBeforeLimitFilterImpl.class);

    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String serviceName = rpcInvocation.getTargetServiceName();
        ServerServiceSemaphoreWrapper serverServiceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName);
        // 从缓存中提取semaphore对象
        Semaphore semaphore = serverServiceSemaphoreWrapper.getSemaphore();
        boolean tryResult = semaphore.tryAcquire();
        if (!tryResult) {
            LOGGER.error("[ServerServiceBeforeLimitFilterImpl] {}'s max request is {},reject now",
                    rpcInvocation.getTargetServiceName(), serverServiceSemaphoreWrapper.getMaxNums());
            MaxServiceLimitRequestException iRpcException = new MaxServiceLimitRequestException(rpcInvocation);
            rpcInvocation.setE(iRpcException);
            throw iRpcException;
        }
    }
}
