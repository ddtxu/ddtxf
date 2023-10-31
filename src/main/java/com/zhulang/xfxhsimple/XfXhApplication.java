package com.zhulang.xfxhsimple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author
 * @create
 */
@SpringBootApplication
@Configuration
public class XfXhApplication {
    public static void main(String[] args) {
        SpringApplication.run(XfXhApplication.class, args);
    }
}
