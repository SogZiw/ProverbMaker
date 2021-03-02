package com.song.proverbmaker.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 只能用于{@link android.view.View.OnClickListener onClick()}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SingleClick {

    /**
     * 快速点击的间隔
     */
    long value() default 1000;
}