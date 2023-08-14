package cn.bobasyu.core.proxy;

public interface ProxyFactory {
    <T> T getProxy(final Class clazz) throws Exception;
}
