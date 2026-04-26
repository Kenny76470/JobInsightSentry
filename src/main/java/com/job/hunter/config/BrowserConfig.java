package com.job.hunter.config;

import com.microsoft.playwright.*;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class BrowserConfig {

    // --- Playwright 相關組件 ---

    @Bean
    public Playwright playwright() {
        return Playwright.create();
    }

    @Bean
    public Browser browser(Playwright playwright) {
        // 設為 false 方便開發時觀察爬蟲行為
        // 將 setHeadless(true) 改成 false
        return playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)  // 💡 讓瀏覽器視窗彈出來
                .setSlowMo(100));    // 💡 稍微放慢動作讓我們看得清
    }

    // --- 網路連線相關組件 ---

    @Bean
    public OkHttpClient okHttpClient() {
        // 定義一個單例的 OkHttpClient 給 GeminiService 使用
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}