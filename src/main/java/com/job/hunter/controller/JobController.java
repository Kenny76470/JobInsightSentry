package com.job.hunter.controller;

import com.job.hunter.model.JobDetail;
import com.job.hunter.service.CrawlerService;
import com.job.hunter.service.GeminiService;
import com.job.hunter.util.ContentFilter;
import com.job.hunter.util.FileUtil;      // 👈 記得 import
import com.job.hunter.util.UrlValidator;  // 👈 記得 import
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {

    private final CrawlerService crawlerService;
    private final GeminiService geminiService;

    public JobController(CrawlerService crawlerService, GeminiService geminiService) {
        this.crawlerService = crawlerService;
        this.geminiService = geminiService;
    }

    @GetMapping("/api/scan")
    public String scanJob(@RequestParam String url) {
        try {
            // ✅ TODO: URL 預處理
            // 先清洗網址，去除 utm 等追蹤參數，確保爬蟲拿到的是乾淨的 104 連結
            String cleanUrl = UrlValidator.cleanUrl(url);
            if (cleanUrl == null) {
                return "❌ 網址格式錯誤：目前僅支援 104 人力銀行職缺頁面。";
            }

            // 1. 執行爬蟲抓取資料
            JobDetail detail = crawlerService.scrape(cleanUrl);

            // 2. 基本過濾 (黑名單/內容過短)
            if (ContentFilter.isGarbage(detail)) {
                return "🚫 偵測到關鍵字或內容異常，已自動攔截該職缺。";
            }

            // 3. 整理內容給 AI
            String prompt = String.format("公司：%s, 職稱：%s, 內容：%s",
                    detail.companyName(), detail.jobTitle(), detail.content());

            // 4. 呼叫 AI 分析
            String aiResult = geminiService.analyze(prompt);

            // ✅ TODO: 補齊存檔邏輯
            // 將原始職缺資訊與 AI 報告分別存檔，方便後續查閱
            FileUtil.saveJob(detail, cleanUrl);
            FileUtil.saveAnalysis(detail, aiResult);

            return String.format("""
                    ✅ 掃描完成並已自動存檔！
                    
                    【職缺資訊】
                    公司：%s
                    職稱：%s
                    
                    【🤖 Gemini AI 鑑定報告】
                    %s
                    """, detail.companyName(), detail.jobTitle(), aiResult);

        } catch (Exception e) {
            // 資深工程師碎碎念：生產環境建議用 Log 框架，這裡先用 printStackTrace 除錯
            e.printStackTrace();
            return "❌ 系統掃描發生異常：" + e.getMessage();
        }
    }
}