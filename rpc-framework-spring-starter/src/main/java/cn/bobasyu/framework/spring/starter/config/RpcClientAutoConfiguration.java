package cn.bobasyu.framework.spring.starter.config;

import cn.bobasyu.core.client.Client;
import cn.bobasyu.core.client.ConnectionHandler;
import cn.bobasyu.core.client.RpcReferenceWrapper;
import cn.bobasyu.framework.spring.starter.common.RpcReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Field;

/**
 * rpc客户端的自动装配类
 */
public class RpcClientAutoConfiguration implements BeanPostProcessor, ApplicationListener<ApplicationReadyEvent> {

    private static cn.bobasyu.core.client.RpcReference rpcReference = null;
    private static Client client = null;
    private volatile boolean needInitClient = false;
    private volatile boolean hasInitClientConfig = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientAutoConfiguration.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(RpcReference.class)) {
                if (!hasInitClientConfig) {
                    //初始化客户端的配置
                    client = new Client();
                    try {
                        rpcReference = client.initClientApplication();
                    } catch (Exception e) {
                        LOGGER.error("[IRpcClientAutoConfiguration] postProcessAfterInitialization has error ", e);
                        throw new RuntimeException(e);
                    }
                    hasInitClientConfig = true;
                }
                needInitClient = true;
                RpcReference rpcReferenceEnum = field.getAnnotation(RpcReference.class);
                try {
                    field.setAccessible(true);
                    Object refObj;
                    RpcReferenceWrapper rpcReferenceWrapper = new RpcReferenceWrapper();
                    rpcReferenceWrapper.setAimClass(field.getType());
                    rpcReferenceWrapper.setGroup(rpcReferenceEnum.group());
                    rpcReferenceWrapper.setServiceToken(rpcReferenceEnum.serviceToken());
                    rpcReferenceWrapper.setUrl(rpcReferenceEnum.url());
                    rpcReferenceWrapper.setTimeOut(rpcReferenceEnum.timeOut());
                    //失败重试次数
                    rpcReferenceWrapper.setRetry(rpcReferenceEnum.retry());
                    rpcReferenceWrapper.setAsync(rpcReferenceEnum.async());
                    refObj = rpcReference.get(rpcReferenceWrapper);
                    field.set(bean, refObj);
                    client.doSubscribeService(field.getType());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
        return bean;
    }


    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        if (needInitClient && client != null) {
            LOGGER.info(" ================== [{}] started success ================== ", client.getClientConfig().getApplicationName());
            ConnectionHandler.setBootstrap(client.getBootstrap());
            client.doConnectServer();
            client.startClient();
        }
    }
}