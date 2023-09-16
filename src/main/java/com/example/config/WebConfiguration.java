package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 防止依赖循环
 * 配置BC密码加密器
 * @author blbyd_li
 * @data 2023/9/10
 * @apiNote
 */
@Configuration
public class WebConfiguration {
    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
