package com.job.hunter.controller;

import com.job.hunter.model.JobDetail;
import com.job.hunter.model.UserConfigEntity;
import com.job.hunter.repository.UserConfigRepository;
import com.job.hunter.service.AiService;
import com.job.hunter.service.CrawlerService;
import com.job.hunter.util.ContentFilter;
import com.job.hunter.util.FileUtil;
import com.job.hunter.util.UrlValidator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class JobController {

    private final CrawlerService crawlerService;
    private final AiService aiService;
    // 💡 注入 Repository，Spring 會自動從 repository package 找來用
    private final UserConfigRepository userConfigRepository;

    public JobController(CrawlerService crawlerService,
                         AiService aiService,
                         UserConfigRepository userConfigRepository) {
        this.crawlerService = crawlerService;
        this.aiService = aiService;
        this.userConfigRepository = userConfigRepository;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 🚀 新增：儲存使用者配置的 API
     * 讓你在 index.html 填完條件後，點擊「儲存」會跑到這裡
     */
    @PostMapping("/api/config/save")
    public String saveConfig(@RequestParam String username,
                             @RequestParam String searchKeyword,
                             @RequestParam int minSalary,
                             @RequestParam String lineUserId) {

        // 呼叫你在 Repository 寫的 findByUsername
        UserConfigEntity config = userConfigRepository.findByUsername(username);
        if (config == null) {
            config = new UserConfigEntity();
            config.setUsername(username);
        }

        config.setSearchKeyword(searchKeyword);
        config.setMinSalary(minSalary);
        config.setLineUserId(lineUserId);

        userConfigRepository.save(config);

        return "redirect:/?status=success";
    }

    @GetMapping("/scan")
    public ModelAndView scanJob(@RequestParam String url) {
        ModelAndView mav = new ModelAndView("index");
        // ... (你原本的 scan 邏輯保持不變) ...
        try {
            String cleanUrl = UrlValidator.cleanUrl(url);
            if (cleanUrl == null) {
                mav.addObject("result", "❌ 網址格式錯誤：目前僅支援 104 人力銀行職缺頁面。");
                return mav;
            }
            JobDetail detail = crawlerService.scrape(cleanUrl);
            if (detail == null) {
                mav.addObject("result", "ℹ️ 此職缺已存在於資料庫中，系統自動跳過重複分析。");
                return mav;
            }
            if (ContentFilter.isGarbage(detail)) {
                mav.addObject("result", "🚫 偵測到關鍵字或內容異常，系統已自動攔截該職缺。");
                return mav;
            }
            String prompt = String.format("公司：%s, 職稱：%s, 內容：%s",
                    detail.companyName(), detail.jobTitle(), detail.content());
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