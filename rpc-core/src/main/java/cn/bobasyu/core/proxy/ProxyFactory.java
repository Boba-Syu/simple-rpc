package cn.bobasyu.core.proxy;

import cn.bobasyu.core.client.RpcReferenceWrapper;

public interface ProxyFactory {
    <T> T getProxy(RpcReferenceWrapper<?> clazz) throws Exception;
}
