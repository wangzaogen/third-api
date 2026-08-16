package com.thirdapi.sdk.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java interface as a third-party API client.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ThirdPartyApi {

    /**
     * Provider code, e.g. sms, payment, identity.
     */
    String provider();

    /**
     * Channel code, e.g. aliyun-sms, tencent-sms.
     */
    String channel() default "";

    /**
     * Static base URL. Leave empty when the URL is managed by the admin console.
     */
    String baseUrl() default "";
}
