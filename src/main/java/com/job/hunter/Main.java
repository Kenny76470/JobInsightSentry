package com.job.hunter;

import com.job.hunter.config.BrowserConfig;
import com.job.hunter.model.JobDetail;
import com.job.hunter.service.CrawlerService;
import com.job.hunter.service.GeminiService;
import com.job.hunter.util.FileUtil;
import com.job.hunter.util.UrlValidator; // 確保這行有 import
import com.microsoft.playwright.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try (Playwright playwright = Playwright.create()) {
            BrowserContext context = BrowserConfig.createContext(playwright);
            Page page = context.newPage();
            CrawlerService crawler = new CrawlerService(page);

            while (true) {
                System.out.println("\n==================================================");
                System.out.print("請輸入 104 職缺網址 (輸入 'exit' 結束): ");
                String inputUrl = scanner.nextLine();

                // 1. 檢查是否要結束
                if ("exit".equalsIgnoreCase(inputUrl)) break;

                // 2. 呼叫清洗器 (修正：變數名改為 targetUrl，不要重複宣告)
                String targetUrl = UrlValidator.cleanUrl(inputUrl);

                if (targetUrl == null) {
                    System.err.println("❌ 網址格式錯誤！這不是有效的 104 職缺連結。");
                    continue;
                }

                try {
                    // 3. 抓取數據
                    System.out.println("⏳ 正在爬取資料...");
                    JobDetail job = crawler.scrape(targetUrl);

                    // 4. 打印摘要
                    System.out.println("\n✅ [抓取成功] " + job.companyName() + " - " + job.jobTitle());

                    // 5. 存檔與 AI 分析
                    FileUtil.saveJob(job, targetUrl);

                    System.out.println("🤖 【AI 分析中】請稍候...");
                    String analysisReport = GeminiService.analyze(job);

                    System.out.println("\n--- AI 職缺鑑定報告 ---");
                    System.out.println(analysisReport);
                    System.out.println("----------------------\n");

                    FileUtil.saveAnalysis(job, analysisReport);

                } catch (Exception e) {
                    System.err.println("❌ 處理失敗: " + e.getMessage());
                }
            }
            context.browser().close();
        } catch (Exception e) {
            System.err.println("❌ 啟動失敗: " + e.getMessage());
        }
    }
}