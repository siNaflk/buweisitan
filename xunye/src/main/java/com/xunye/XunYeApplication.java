package com.xunye;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 寻页启动类
 */
@SpringBootApplication
@MapperScan("com.xunye.dao")
public class XunYeApplication {

    public static void main(String[] args) {
        SpringApplication.run(XunYeApplication.class, args);
    }
}
