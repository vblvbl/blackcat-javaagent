package com.github.bingoohuang.blackcat.javaagent;


import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlackcatCreateTransformedClassFile {
}
