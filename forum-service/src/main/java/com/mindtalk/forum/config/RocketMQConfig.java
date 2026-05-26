package com.mindtalk.forum.config;

import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置
 * Producer / Consumer 由 rocketmq-spring-boot-starter 自动配置，
 * 无需额外定义 RocketMQTemplate Bean。
 */
@Configuration
public class RocketMQConfig {
    // RocketMQTemplate 由自动配置提供，直接注入即可
}
