package cn.bobasyu.core.filter.server;

import cn.bobasyu.core.annotation.SPI;
import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.filter.ServerFilter;
import cn.bobasyu.core.server.ServiceWrapper;
import cn.bobasyu.core.utils.CommonUtils;

import static cn.bobasyu.core.common.cache.CommonServerCache.PROVIDER_SERVICE_WRAPPER_MAP;

/**
 * 简单版本的token校验过滤器
 */
@SPI("before")
public class ServerTokenFilterImpl implements ServerFilter {
    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String token = String.valueOf(rpcInvocation.getAttachments().get("serviceToken"));
        ServiceWrapper serviceWrapper = PROVIDER_SERVICE_WRAPPER_MAP.get(rpcInvocation.getTargetServiceName());
        String matchToken = String.valueOf(serviceWrapper.getServiceToken());
        if (CommonUtils.isEmpty(matchToken)) {
            return;
        }
        if (!CommonUtils.isEmpty(token) && token.equals(matchToken)) {
            return;
        }
        throw new RuntimeException("token is " + token + " , verify result is false!");
    }
}
