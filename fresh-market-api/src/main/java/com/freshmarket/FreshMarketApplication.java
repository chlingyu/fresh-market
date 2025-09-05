package com.freshmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Fresh Market Application Main Class
 * 
 * 生鲜电商系统主启动类
 * - 支持异步任务处理
 * - 支持定时任务调度
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class FreshMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreshMarketApplication.class, args);
    }
}