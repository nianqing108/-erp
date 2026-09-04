package com.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * 简易ERP 后端启动类。
 */
@SpringBootApplication
public class ErpApplication {

    static {
        // 与数据库 serverTimezone、Docker 容器 TZ 保持一致，避免日期错乱
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }
}
