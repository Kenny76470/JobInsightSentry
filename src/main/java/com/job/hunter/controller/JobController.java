package com.job.hunter.controller;

import com.job.hunter.model.JobDetail;
import com.job.hunter.service.AiService; // 👈 1. 這裡要改 import 介面
import com.job.hunter.service.CrawlerService;
import com.job.hunter.util.ContentFilter;
import com.job.hunter.util.FileUtil;
import com.job.hunter.util.UrlValidator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class JobController {

    private final CrawlerService crawlerService;
    private final AiService aiService; // 👈 2. 型態從 GeminiService 改為 AiService 介面

    // 👈 3. 建構子注入的參數也要改成 AiService
    public JobController(CrawlerService crawlerService, AiService aiService) {
        this.crawlerService = crawlerService;
        this.aiService = aiService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/scan")
    public ModelAndView scanJob(@RequestParam String url) {
        ModelAndView mav = new ModelAndView("index");

        try {
            String cleanUrl = UrlValidator.cleanUrl(url);
            if (cleanUrl == null) {
                mav.addObject("result", "❌ 網址格式錯誤：目前僅支援 104 人力銀行職缺頁面。");
                return mav;
            }

            JobDetail detail = crawlerService.scrape(cleanUrl);

            if (ContentFilter.isGarbage(detail)) {
                mav.addObject("result", "🚫 偵測到關鍵字或內容異常，系統已自動攔截該職缺。");
                return mav;
            }

            String prompt = String.format("公司：%s, 職稱：%s, 內容：%s",
                    detail.companyName(), detail.jobTitle(), detail.content());

            // 👈 4. 這裡會自動根據 Profile 呼叫到對應的 analyze 實作
            String aiResult = aiService.analyze(prompt);

            mav.addObject("companyName", detail.companyName());
            mav.addObject("jobTitle", detail.jobTitle());
            mav.addObject("result", aiResult);
            mav.addObject("scanTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            FileUtil.saveJob(detail, cleanUrl);
            FileUtil.saveAnalysis(detail, aiResult);

        } catch (Exception e) {
            e.printStackTrace();
            mav.addObject("result", "❌ 系統掃描發生異常：" + e.getMessage());
        }

        return mav;
    }
}