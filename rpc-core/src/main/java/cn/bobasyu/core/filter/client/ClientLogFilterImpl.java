package cn.bobasyu.core.filter.client;

import cn.bobasyu.core.common.ChannelFutureWrapper;
import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.filter.ClientFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static cn.bobasyu.core.common.cache.CommonClientCache.CLIENT_CONFIG;

/**
 * 客户端日志记录过滤器
 */
public class ClientLogFilterImpl implements ClientFilter {
    private static Logger logger = LoggerFactory.getLogger(ClientLogFilterImpl.class);

    /**
     * 记录当前客户端程序调用了哪个具体的service方法
     *
     * @param src
     * @param rpcInvocation
     */
    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        rpcInvocation.getAttachments().put("c_app_name", CLIENT_CONFIG.getApplicationName());
        logger.info(rpcInvocation.getAttachments().get("c_app_name") + " do invoke ----->" + rpcInvocation.getTargetServiceName());
    }
}
