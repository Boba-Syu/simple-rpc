package cn.bobasyu.core.client;

import cn.bobasyu.core.proxy.ProxyFactory;
import cn.bobasyu.core.proxy.jdk.JDKProxyFactory;

public class RpcReference {
    private ProxyFactory proxyFactory;

    public RpcReference(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }
}
