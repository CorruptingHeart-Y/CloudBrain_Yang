package com.neusoft.hospital;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 东软智慧云脑诊疗平台 - 启动类
 */
@SpringBootApplication
@MapperScan("com.neusoft.hospital.mapper")
public class HospitalCloudBrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalCloudBrainApplication.class, args);
    }
}
