package org.jpos.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.jpos.util.JsonLogWriter;

public class SpringContextHolder {
    public static final AnnotationConfigApplicationContext context;
    static {
        context = new AnnotationConfigApplicationContext(SpringConfig.class);
    }
}
