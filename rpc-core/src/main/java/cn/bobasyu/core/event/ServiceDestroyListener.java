package cn.bobasyu.core.event;

import cn.bobasyu.core.common.event.RpcListener;
import cn.bobasyu.core.registry.URL;

import static cn.bobasyu.core.common.cache.CommonServerCache.PROVIDER_URL_SET;
import static cn.bobasyu.core.common.cache.CommonServerCache.REGISTRY_SERVICE;

public class ServiceDestroyListener implements RpcListener {
    @Override
    public void callback(Object t) {
        for (URL url : PROVIDER_URL_SET) {
            REGISTRY_SERVICE.unRegister(url);
        }
    }
}