package com.example.mydatabackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.mydatabackend.mapper")
@SpringBootApplication
public class MydataBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MydataBackendApplication.class, args);
    }
}
