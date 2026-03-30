package com.sinolink.channel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sinolink.channel.mapper")
public class ChannelPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChannelPlatformApplication.class, args);
    }
}
