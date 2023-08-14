package cn.bobasyu.core.common.event;

import cn.bobasyu.core.utils.CommonUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 事件管理类，负责发送事件
 */
public class RpcListenerLoader {

    private static List<RpcListener> rpcListenerList = new ArrayList<>();

    private static ExecutorService eventThreadPool = Executors.newFixedThreadPool(2);

    public static void registerListener(RpcListener listener) {
        rpcListenerList.add(listener);
    }

    public void init() {
        registerListener(new ServiceUpdateListener());
    }

    /**
     * 获取接口上的泛型T
     *
     * @param o 接口
     */
    public static Class<?> getInterfaceT(Object o) {
        Type[] types = o.getClass().getGenericInterfaces();
        ParameterizedType parameterizedType = (ParameterizedType) types[0];
        Type type = parameterizedType.getActualTypeArguments()[0];
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        return null;
    }

    /**
     * 发送事件，同时触发监听
     *
     * @param rpcEvent
     */
    public static void sendEvent(RpcEvent rpcEvent) {
        if (CommonUtils.isEmptyList(rpcListenerList)) {
            return;
        }
        for (RpcListener<?> listener : rpcListenerList) {
            Class<?> type = getInterfaceT(listener);
            if (type.equals(rpcEvent.getData())) {
                eventThreadPool.execute(() -> {
                    try {
                        listener.callback(rpcEvent.getData());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
