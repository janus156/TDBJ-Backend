package com.tdbj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.tdbj.mapper")
@SpringBootApplication
public class TdbjPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TdbjPingApplication.class, args);
    }

}
