package cn.bobasyu.framework.spring.starter.common;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcReference {

    String url() default "";

    /**
     * 服务分组
     *
     * @return
     */
    String group() default "default";

    /**
     * 服务的令牌校验
     *
     * @return
     */
    String serviceToken() default "";

    /**
     * 服务调用的超时时间
     *
     * @return
     */
    int timeOut() default 3000;

    /**
     * 重试次数
     *
     * @return
     */
    int retry() default 1;

    /**
     * 是否异步调用
     *
     * @return
     */
    boolean async() default false;
}
