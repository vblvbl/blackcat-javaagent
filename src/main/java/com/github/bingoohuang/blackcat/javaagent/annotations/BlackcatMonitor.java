package com.github.bingoohuang.blackcat.javaagent.annotations;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BlackcatMonitor {
}
