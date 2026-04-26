package com.job.hunter.service;

import com.job.hunter.model.JobDetail;
import com.job.hunter.model.UserConfigEntity;
import com.job.hunter.repository.UserConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class JobScheduler {

    @Autowired
    private UserConfigRepository userConfigRepository;

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private JobAnalysisService jobAnalysisService;

    // 每一小時執行一次
    //@Scheduled(cron = "0 0 * * * ?")
    // 💡 開發模式：每 30 秒執行一次
    @Scheduled(fixedRate = 30000)
    public void runAllTasks() {
        List<UserConfigEntity> users = userConfigRepository.findAll();
        log.info("=== [定時任務啟動] 目前共有 {} 位使用者待掃描 ===", users.size());

        for (UserConfigEntity user : users) {
            try {
                processUserWorkflow(user);
            } catch (Exception e) {
                log.error("使用者 [{}] 執行過程中發生錯誤: {}", user.getUsername(), e.getMessage());
            }
        }
        log.info("=== [定時任務結束] 所有人掃描完畢 ===");
    }

    private void processUserWorkflow(UserConfigEntity user) {
        log.info(">>> 正在處理使用者 [{}], 關鍵字: {}", user.getUsername(), user.getSearchKeyword());

        // 💡 關鍵修正：對關鍵字進行 URL 編碼，處理空格與特殊字元
        String encodedKeyword = URLEncoder.encode(user.getSearchKeyword(), StandardCharsets.UTF_8);
        String searchUrl = "https://www.104.com.tw/jobs/search/?keyword=" + encodedKeyword;

        List<String> jobUrls = crawlerService.scrapeJobList(searchUrl);
        log.info("找到 {} 個相關職缺 URL", jobUrls.size());

        int count = 0;
        for (String url : jobUrls) {
            // 2. 爬取詳情 (內部已處理資料庫重複檢查)
            JobDetail detail = crawlerService.scrape(url);

            if (detail != null) {
                count++;
                log.info("發現新職缺: {} @ {}，準備進入 AI 分析...", detail.jobTitle(), detail.companyName());

                // 3. 立即進行 AI 分析與回填資料庫
                jobAnalysisService.analyzeAndBackfill(url);
            }
        }

        log.info("使用者 [{}] 掃描完成，共新增並分析 {} 筆新職缺。", user.getUsername(), count);
    }
}