package cn.bobasyu.framework.spring.starter.common;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RpcService {

    int limit() default 0;

    String group() default "default";

    String serviceToken() default "";
}
