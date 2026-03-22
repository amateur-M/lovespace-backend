package com.meng.lovespace.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.mybatis.spring.annotation.MapperScan;

/**
 * LoveSpace 用户服务启动类。
 *
 * <p>负责扫描 MyBatis Mapper、加载 {@code com.meng.lovespace.user.config} 下的配置属性类。
 */
@SpringBootApplication
@MapperScan("com.meng.lovespace.user.mapper")
@ConfigurationPropertiesScan("com.meng.lovespace.user.config")
public class LoveSpaceUserApplication {

    /**
     * 应用入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(LoveSpaceUserApplication.class, args);
    }
}

