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

    // 💡 測試時可以先用 30000 (30秒)，確定沒問題要放著掛機時，再改回 300000 (5分鐘)
    @Scheduled(fixedDelay = 30000)
    public void runAllTasks() {
        List<UserConfigEntity> users = userConfigRepository.findAll();

        if (users.isEmpty()) {
            log.info("🕒 目前尚無使用者配置，等待網頁端設定中...");
            return;
        }

        log.info("=== [定時任務啟動] 目前共有 {} 位使用者待掃描 ===", users.size());

        for (UserConfigEntity user : users) {
            try {
                processUserWorkflow(user);
            } catch (Exception e) {
                log.error("使用者 [{}] 執行過程中發生錯誤: {}", user.getUsername(), e.getMessage());
            }
        }
        log.info("=== [定時任務結束] 所有人掃描完畢，進入待機 ===");
    }

    private void processUserWorkflow(UserConfigEntity user) {
        log.info(">>> 正在處理使用者 [{}], 關鍵字: {}", user.getUsername(), user.getSearchKeyword());

        String encodedKeyword = URLEncoder.encode(user.getSearchKeyword(), StandardCharsets.UTF_8);
        String searchUrl = "https://www.104.com.tw/jobs/search/?keyword=" + encodedKeyword;

        // 1. 抓取列表
        List<String> jobUrls = crawlerService.scrapeJobList(searchUrl);
        log.info("找到 {} 個相關職缺 URL", jobUrls.size());

        int count = 0;
        for (String url : jobUrls) {
            // 2. 爬取詳情
            JobDetail detail = crawlerService.scrape(url);

            if (detail != null) {
                count++;
                log.info("發現新職缺: {} @ {}，準備進入 AI 分析...", detail.jobTitle(), detail.companyName());

                // 💡 關鍵修正就是在這行！加上 user.getLineUserId()
                boolean isSuccess = jobAnalysisService.analyzeAndBackfill(url, user.getLineUserId());

                // 配合 Gemini API 的冷卻機制 (成功 10 秒，被擋 60 秒)
                try {
                    int sleepTime = isSuccess ? 10000 : 60000;
                    log.info("⏳ AI 分析結束，系統冷卻 {} 秒...", sleepTime / 1000);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("睡眠被打斷", e);
                }
            }
        }

        log.info("使用者 [{}] 掃描完成，本次共新增並分析 {} 筆新職缺。", user.getUsername(), count);
    }
}