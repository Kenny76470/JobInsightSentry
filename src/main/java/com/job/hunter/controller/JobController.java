package com.job.hunter.controller;

import com.job.hunter.model.JobDetail;
import com.job.hunter.service.AiService;
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
    private final AiService aiService;

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

            // 1. 執行爬蟲（內部會先去資料庫查重）
            JobDetail detail = crawlerService.scrape(cleanUrl);

            // 🛡️ 2. 【新增防禦性判斷】處理重複職缺
            if (detail == null) {
                mav.addObject("result", "ℹ️ 此職缺已存在於資料庫中，系統自動跳過重複分析。");
                return mav;
            }

            // 3. 過濾垃圾資訊（現在保證 detail 不是 null 了）
            if (ContentFilter.isGarbage(detail)) {
                mav.addObject("result", "🚫 偵測到關鍵字或內容異常，系統已自動攔截該職缺。");
                return mav;
            }

            // 4. AI 分析
            String prompt = String.format("公司：%s, 職稱：%s, 內容：%s",
                    detail.companyName(), detail.jobTitle(), detail.content());

            String aiResult = aiService.analyze(prompt);

            // 5. 渲染頁面數據
            mav.addObject("companyName", detail.companyName());
            mav.addObject("jobTitle", detail.jobTitle());
            mav.addObject("result", aiResult);
            mav.addObject("scanTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 6. 存檔（CSV/TXT）
            FileUtil.saveJob(detail, cleanUrl);
            FileUtil.saveAnalysis(detail, aiResult);

        } catch (Exception e) {
            e.printStackTrace();
            mav.addObject("result", "❌ 系統掃描發生異常：" + e.getMessage());
        }

        return mav;
    }
}