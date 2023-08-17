package cn.bobasyu.framework.spring.starter.config;


import cn.bobasyu.core.common.event.RpcListenerLoader;
import cn.bobasyu.core.server.ApplicationShutdownHook;
import cn.bobasyu.core.server.Server;
import cn.bobasyu.core.server.ServiceWrapper;
import cn.bobasyu.framework.spring.starter.common.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * rpc服务端的自动装配类
 */
public class RpcServerAutoConfiguration implements InitializingBean, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerAutoConfiguration.class);

    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        Server server = null;
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (beanMap.size() == 0) {
            //说明当前应用内部不需要对外暴露服务，无需执行下边多余的逻辑
            return;
        }
        long begin = System.currentTimeMillis();
        server = new Server();
        RpcListenerLoader rpcListenerLoader = new RpcListenerLoader();
        rpcListenerLoader.init();
        for (String beanName : beanMap.keySet()) {
            Object bean = beanMap.get(beanName);
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            ServiceWrapper dataServiceWrapper = new ServiceWrapper(bean, rpcService.group());
            dataServiceWrapper.setServiceToken(rpcService.serviceToken());
            dataServiceWrapper.setLimit(rpcService.limit());
            LOGGER.info(">>>>>>>>>>>>>>> [rpc] {} export success! >>>>>>>>>>>>>>> ",beanName);
        }    long end = System.currentTimeMillis();
        ApplicationShutdownHook.registryShutdownHook();
        server.startApplication();
        LOGGER.info(" ================== [{}] started success in {}s ================== ",server.getServerConfig().getApplicationName(),((double)end-(double)begin)/1000);

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
