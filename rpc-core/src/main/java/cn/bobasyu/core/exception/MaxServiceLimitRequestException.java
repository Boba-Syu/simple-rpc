package cn.bobasyu.core.exception;

import cn.bobasyu.core.common.RpcInvocation;

public class MaxServiceLimitRequestException extends RpcException {

    public MaxServiceLimitRequestException(RpcInvocation rpcInvocation) {
        super(rpcInvocation);
    }
}
