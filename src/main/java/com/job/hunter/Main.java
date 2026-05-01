package com.job.hunter;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main {

    static {
        // 💡 核心修正：在 Spring Boot 啟動前手動加載 .env 檔案
        // 這樣 @Value("${LINE_CHANNEL_TOKEN}") 才能正確抓到值
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // 如果找不到檔案則忽略，避免開發環境報錯
                    .load();

            // 將 .env 中的每一項設定存入系統屬性中
            dotenv.entries().forEach(entry ->
                    System.setProperty(entry.getKey(), entry.getValue())
            );
            System.out.println("✅ 已加載 .env 環境變數");
        } catch (Exception e) {
            System.err.println("⚠️ 加載 .env 檔案失敗：" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        System.out.println("🚀 JobInsightSentry 啟動成功！");
    }
}