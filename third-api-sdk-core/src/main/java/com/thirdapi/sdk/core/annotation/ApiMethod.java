package com.thirdapi.sdk.core.annotation;

import com.thirdapi.sdk.core.model.HttpMethod;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明第三方 API 方法对应的 HTTP 契约。
 *
 * <p>方法名与请求路径、HTTP 方法、超时和重试策略一起组成端点定义，
 * 管理端配置通过端点编码与这里声明的契约进行对齐。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMethod {

    /**
     * 端点编码，作为管理端配置中的稳定标识。
     */
    String name() default "";

    /**
     * 请求路径，例如 /v1/sms/send，可使用 {参数名} 占位符。
     */
    String path();

    /**
     * HTTP 方法，默认 POST。
     */
    HttpMethod method() default HttpMethod.POST;

    /**
     * 超时时间（毫秒），小于 0 时使用全局默认值。
     */
    int timeoutMs() default -1;

    /**
     * 最大重试次数，小于 0 时使用全局默认值。
     */
    int maxRetries() default -1;
}
