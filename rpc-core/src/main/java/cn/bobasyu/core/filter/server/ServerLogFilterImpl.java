package cn.bobasyu.core.filter.server;

import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.filter.ServerFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务端日志记录过滤器
 */
public class ServerLogFilterImpl implements ServerFilter {
    private static Logger logger = LoggerFactory.getLogger(ServerLogFilterImpl.class);

    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        logger.info(rpcInvocation.getAttachments().get("c_app_name") + " do invoke -----> "
                + rpcInvocation.getTargetServiceName() + "#" + rpcInvocation.getTargetMethod());

    }
}
