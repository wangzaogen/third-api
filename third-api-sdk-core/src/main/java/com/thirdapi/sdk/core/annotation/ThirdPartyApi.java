package com.thirdapi.sdk.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个 Java 接口为第三方 API 客户端。
 *
 * <p>标注该注解的接口会由 Starter 在运行时生成 JDK 动态代理，
 * 业务代码只需注入接口即可发起第三方接口调用。</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ThirdPartyApi {

    /**
     * 服务商编码，例如 sms（短信）、payment（支付）、identity（身份认证）。
     */
    String provider();

    /**
     * 渠道编码，例如 aliyun-sms、tencent-sms；为空时表示默认渠道。
     */
    String channel() default "";

    /**
     * 静态基础地址；留空时由管理端配置动态下发。
     */
    String baseUrl() default "";
}
