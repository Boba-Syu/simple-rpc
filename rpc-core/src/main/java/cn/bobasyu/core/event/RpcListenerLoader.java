package cn.bobasyu.core.event;


import cn.bobasyu.core.common.event.ProviderNodeDataChangeListener;
import cn.bobasyu.core.common.event.RpcEvent;
import cn.bobasyu.core.common.event.RpcListener;
import cn.bobasyu.core.common.event.ServiceUpdateListener;
import cn.bobasyu.core.utils.CommonUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RpcListenerLoader {

    private static List<RpcListener> rpcListenerList = new ArrayList<>();

    private static ExecutorService eventThreadPool = Executors.newFixedThreadPool(2);

    public static void registerListener(RpcListener iRpcListener) {
        rpcListenerList.add(iRpcListener);
    }

    public void init() {
        registerListener(new ServiceUpdateListener());
        registerListener(new ServiceDestroyListener());
        registerListener(new ProviderNodeDataChangeListener());
    }

    public static Class<?> getInterfaceT(Object o) {
        Type[] types = o.getClass().getGenericInterfaces();
        ParameterizedType parameterizedType = (ParameterizedType) types[0];
        Type type = parameterizedType.getActualTypeArguments()[0];
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        return null;
    }

    public static void sendSyncEvent(RpcEvent rpcEvent) {
        if (CommonUtils.isEmptyList(rpcListenerList)) {
            return;
        }
        for (RpcListener<?> rpcListener : rpcListenerList) {
            Class<?> type = getInterfaceT(rpcListener);
            if (type.equals(rpcEvent.getClass())) {
                try {
                    rpcListener.callback(rpcEvent.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void sendEvent(RpcEvent rpcEvent) {
        if (CommonUtils.isEmptyList(rpcListenerList)) {
            return;
        }
        for (RpcListener<?> rpcListener : rpcListenerList) {
            Class<?> type = getInterfaceT(rpcListener);
            if (type.equals(rpcEvent.getClass())) {
                eventThreadPool.execute(() -> {
                    try {
                        rpcListener.callback(rpcEvent.getData());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
