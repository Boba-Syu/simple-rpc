package cn.bobasyu.core.registry.zookeeper;

import cn.bobasyu.core.registry.RegistryService;
import cn.bobasyu.core.registry.URL;

import java.util.List;

import static cn.bobasyu.core.common.cache.CommonClientCache.SUBSCRIBE_SERVICE_LIST;
import static cn.bobasyu.core.common.cache.CommonServerCache.PROVIDER_URL_SET;

/**
 * 对注册数据进行统一处理，所有基础的记录操作都可以统一放在抽象类里实现
 */
public abstract class AbstractRegister implements RegistryService {
    @Override
    public void register(URL url) {
        PROVIDER_URL_SET.add(url);
    }

    @Override
    public void unRegister(URL url) {
        PROVIDER_URL_SET.remove(url);
    }

    @Override
    public void subscribe(URL url) {
        SUBSCRIBE_SERVICE_LIST.add(url.getServiceName());
    }

    @Override
    public void doUnSubscribe(URL url) {
        SUBSCRIBE_SERVICE_LIST.remove(url.getServiceName());
    }

    public abstract void doBeforeSubscribe(URL url);

    public abstract void doAfterSubscribe(URL url);

    public abstract List<String> getProviderIps(String serviceName);
}
