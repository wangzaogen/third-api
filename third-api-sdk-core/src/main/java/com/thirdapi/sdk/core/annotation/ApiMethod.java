package com.thirdapi.sdk.core.annotation;

import com.thirdapi.sdk.core.model.HttpMethod;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the HTTP contract of a third-party API method.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMethod {

    /**
     * Endpoint code, used as the stable key in the admin console.
     */
    String name() default "";

    /**
     * Request path, e.g. /v1/sms/send.
     */
    String path();

    /**
     * HTTP method.
     */
    HttpMethod method() default HttpMethod.POST;

    /**
     * Timeout in milliseconds. A negative value falls back to global config.
     */
    int timeoutMs() default -1;

    /**
     * Maximum retry attempts. A negative value falls back to global config.
     */
    int maxRetries() default -1;
}
