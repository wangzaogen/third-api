package com.thirdapi.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables third-api auto configuration on a Spring Boot application.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableThirdPartyApis {

    /**
     * Base packages to scan for interfaces annotated with ThirdPartyApi.
     */
    String[] basePackages() default {};
}
