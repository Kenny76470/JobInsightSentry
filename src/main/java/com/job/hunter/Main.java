package com.job.hunter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // 這是核心魔法：它會自動掃描同個 package 下的所有註解
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        System.out.println("🚀 JobInsightSentry 啟動成功！");
    }
}