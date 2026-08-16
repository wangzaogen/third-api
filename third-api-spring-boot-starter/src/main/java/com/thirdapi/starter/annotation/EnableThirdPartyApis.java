package com.thirdapi.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在 Spring Boot 应用上启用 third-api 自动配置与代理客户端。
 *
 * <p>在配置类或启动类上标注该注解后，Starter 会自动扫描
 * {@code @ThirdPartyApi} 接口并为它们创建可注入的代理实现。</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableThirdPartyApis {

    /**
     * 扫描 {@code @ThirdPartyApi} 接口的基础包；为空时由自动配置按默认规则扫描。
     */
    String[] basePackages() default {};
}
