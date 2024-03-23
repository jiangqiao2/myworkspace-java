package com.easypan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.easypan"})
@MapperScan(basePackages = {"com.easypan.mappers"})
@EnableTransactionManagement
@EnableScheduling
public class EasyPanApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyPanApplication.class, args);
    }


}
