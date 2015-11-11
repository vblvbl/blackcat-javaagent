package com.github.bingoohuang.blackcat.javaagent.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlackcatCreateTransformedClassFile {
}
