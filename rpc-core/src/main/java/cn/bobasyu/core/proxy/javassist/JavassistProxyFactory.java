package cn.bobasyu.core.proxy.javassist;

import cn.bobasyu.core.proxy.ProxyFactory;


public class JavassistProxyFactory implements ProxyFactory {
    @Override
    public <T> T getProxy(Class clazz) throws Exception {
        return (T) ProxyGenerator.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                clazz, new JavassistInvocationHandler(clazz));
    }
}
