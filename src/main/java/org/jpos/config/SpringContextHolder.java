package org.jpos.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringContextHolder {
    public static final AnnotationConfigApplicationContext context;
    static {
        context = new AnnotationConfigApplicationContext(SpringConfig.class);
    }
}
