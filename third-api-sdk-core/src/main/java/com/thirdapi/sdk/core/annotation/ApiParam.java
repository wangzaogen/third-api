package com.thirdapi.sdk.core.annotation;

import com.thirdapi.sdk.core.model.ParamLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method parameter to a path, query, header or body value.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiParam {

    String name() default "";

    ParamLocation location() default ParamLocation.BODY;

    boolean required() default false;
}
