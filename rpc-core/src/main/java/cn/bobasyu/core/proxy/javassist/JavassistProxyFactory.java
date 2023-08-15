package cn.bobasyu.core.proxy.javassist;

import cn.bobasyu.core.client.RpcReferenceWrapper;
import cn.bobasyu.core.proxy.ProxyFactory;


public class JavassistProxyFactory implements ProxyFactory {
    @Override
    public <T> T getProxy(RpcReferenceWrapper<?> rpcReferenceWrapper) throws Exception {
        return (T) ProxyGenerator.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                rpcReferenceWrapper.getAimClass(), new JavassistInvocationHandler(rpcReferenceWrapper));
    }
}
