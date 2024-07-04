package com.yintai.serialnumbergen

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(["com.yintai.serialnumbergen", "com.yintai.shadow", "com.yintai.gateway", "org.nofdev"])
public class Application {
    
    public static void main(String[] args) {
         SpringApplication.run(Application.class, args);
    }
}
