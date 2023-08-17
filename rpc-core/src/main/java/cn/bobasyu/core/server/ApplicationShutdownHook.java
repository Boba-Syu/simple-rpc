package cn.bobasyu.core.server;

import cn.bobasyu.core.event.RpcDestroyEvent;
import cn.bobasyu.core.event.RpcListenerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationShutdownHook {

    public static Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownHook.class);

    public static void registryShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("[registryShutdownHook] ==== ");
                RpcListenerLoader.sendSyncEvent(new RpcDestroyEvent("destroy"));
                System.out.println("destroy");
            }
        }));
    }
}
