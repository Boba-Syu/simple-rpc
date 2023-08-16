package cn.bobasyu.core.exception;

import cn.bobasyu.core.common.RpcInvocation;

public class RpcException extends RuntimeException {

    private RpcInvocation rpcInvocation;

    public RpcException(RpcInvocation rpcInvocation) {
        this.rpcInvocation = rpcInvocation;
    }

    public RpcInvocation getRpcInvocation() {
        return rpcInvocation;
    }

    public void setRpcInvocation(RpcInvocation rpcInvocation) {
        this.rpcInvocation = rpcInvocation;
    }
}
