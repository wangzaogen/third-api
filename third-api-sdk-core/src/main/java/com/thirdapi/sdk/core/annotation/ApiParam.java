package com.thirdapi.sdk.core.annotation;

import com.thirdapi.sdk.core.model.ParamLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将方法参数绑定到 HTTP 请求的路径、查询串、请求头或请求体。
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiParam {

    /**
     * 参数名，用于匹配路径占位符、查询参数名或请求头名。
     */
    String name() default "";

    /**
     * 参数所在位置，默认请求体。
     */
    ParamLocation location() default ParamLocation.BODY;

    /**
     * 是否必填；必填参数为空时调用直接失败。
     */
    boolean required() default false;
}
