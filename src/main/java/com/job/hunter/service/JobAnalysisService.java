package com.job.hunter.service;

import com.google.gson.Gson;
import com.job.hunter.model.AiAnalysisResult;
import com.job.hunter.model.JobEntity;
import com.job.hunter.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class JobAnalysisService {

    private final JobRepository jobRepository;
    private final AiService aiService;
    private final LinePushService linePushService;
    private final Gson gson = new Gson();

    public JobAnalysisService(JobRepository jobRepository,
                              AiService aiService,
                              LinePushService linePushService) {
        this.jobRepository = jobRepository;
        this.aiService = aiService;
        this.linePushService = linePushService;
    }

    /**
     * 批次分析所有尚未評分的職缺 (每 30 分鐘自動執行)
     */
    @Scheduled(fixedDelay = 1800000)
    public void analyzeAllPendingJobs() {
        List<JobEntity> pendingJobs = jobRepository.findTop10ByAiScoreIsNullOrderByCreatedAtDesc();

        if (!pendingJobs.isEmpty()) {
            log.info(">>> [批次補考啟動] 共有 {} 筆待分析職缺", pendingJobs.size());
        }

        for (JobEntity job : pendingJobs) {
            // 補考機制沒有特定使用者的情境，先傳入 null
            boolean success = analyzeAndBackfill(job.getJobUrl(), null);

            if (!success) {
                log.warn(">>> [中斷] 偵測到 API 限制或伺服器滿載，停止本次批次補考任務");
                break;
            }

            try {
                // 每筆分析後強制休息 10 秒
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error(">>> [異常] 執行緒中斷: {}", e.getMessage());
                break;
            }
        }
    }

    @Transactional
    public boolean analyzeAndBackfill(String url, String lineUserId) {
        JobEntity job = jobRepository.findById(url).orElse(null);
        if (job == null) {
            log.warn(">>> 找不到該職缺 URL: {}", url);
            return true;
        }

        // 💡 防護盾 1：如果爬蟲沒抓到內文，直接放棄，不要問 AI！
        if (job.getContent() == null || job.getContent().isBlank()) {
            log.warn(">>> [跳過] 職缺內容為空，爬蟲未成功抓取內文：{}", job.getJobTitle());
            job.setAiScore(-1); // 標記為死檔，未來不再重複分析
            job.setAiAnalysis("爬蟲抓取內文失敗，無資料可供 AI 分析");
            jobRepository.save(job);
            return true; // 回傳 true 讓排程器繼續下一筆，不用冷卻
        }

        log.info(">>> [AI 分析開始] 正在分析：{}", job.getJobTitle());

        try {
            // 💡 防護盾 2：Prompt 加上嚴格的「整數與範圍」限制，防止 AI 亂給小數點
            String prompt = String.format(
                    "你是一位資深 Java 工程師。請分析職缺：【%s @ %s】。內容如下：\n%s\n\n" +
                            "請嚴格以 JSON 格式回傳，不要有 Markdown 標籤：{\"aiScore\": 分數, \"aiAnalysis\": \"評價\"}。\n" +
                            "⚠️ 警告：aiScore 必須是「0 到 100 之間的整數」，絕對不可以有小數點！",
                    job.getJobTitle(), job.getCompanyName(), job.getContent()
            );

            // 呼叫 Ollama
            String jsonResponse = aiService.analyze(prompt);

            // 1. 記錄 AI 原始回傳內容，方便除錯
            log.info(">>> [Ollama 原始回傳]: {}", jsonResponse);

            // 2. 大括號萃取法：專治 AI 前後加廢話的「囉嗦模式」
            if (jsonResponse != null) {
                int startIndex = jsonResponse.indexOf('{');
                int endIndex = jsonResponse.lastIndexOf('}');

                if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
                    // 只截取 { 到 } 之間真正的 JSON 內容
                    jsonResponse = jsonResponse.substring(startIndex, endIndex + 1);
                } else {
                    // 找不到括號，代表 AI 胡言亂語
                    jsonResponse = "";
                }
            }

            // 3. 防呆：如果是空字串
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.error(">>> [失敗] AI 回傳空白內容或找不到 JSON 結構，跳過此筆職缺");
                return true; // 回傳 true 讓外層知道「這筆處理過了，只是失敗」，不要觸發 60 秒冷卻
            }

            // 將 JSON 字串轉為 Java 物件
            AiAnalysisResult result = gson.fromJson(jsonResponse, AiAnalysisResult.class);

            // 4. 防呆：確保轉出來的物件與分數不是 null
            if (result == null || result.aiScore() == null) {
                log.error(">>> [失敗] Gson 無法將字串映射為物件，可能格式錯誤。字串內容: {}", jsonResponse);
                return true;
            }

            // 更新職缺資訊並存入資料庫
            job.setAiScore(result.aiScore());
            job.setAiAnalysis(result.aiAnalysis());
            jobRepository.save(job);

            log.info(">>> [成功] 分數：{}", result.aiScore());

            // 判斷是否大於等於 80 分，是的話就推播 LINE
            if (result.aiScore() >= 80) {
                sendLineNotification(job, result, lineUserId);
            }
            return true;

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            log.error(">>> [失敗] 分析流程異常: {}", errorMsg);

            // 如果又遇到 Google API 的 429 或 503 錯誤 (雖然現在用 Ollama 應該遇不到了，但留著做雙重保險)
            if (errorMsg != null && (errorMsg.contains("429") || errorMsg.contains("RESOURCE_EXHAUSTED") || errorMsg.contains("503"))) {
                log.error(">>> [API 塞車或配額滿了] 系統將暫停並安排補考...");
                return false;
            }

            // 其他未知的嚴重錯誤，寫入 -1 標記為死檔，避免無限重試
            job.setAiScore(-1);
            jobRepository.save(job);
            return true;
        }
    }

    private void sendLineNotification(JobEntity job, AiAnalysisResult result, String lineUserId) {
        // 防呆：如果是來自「批次補考」或是沒填 LINE ID，就不推播
        if (lineUserId == null || lineUserId.isBlank()) {
            log.info(">>> [通知跳過] 沒有提供 LINE User ID，不發送推播");
            return;
        }

        String message = String.format(
                "\n🚀 發現優質職缺 (%d分)！\n" +
                        "🏢 公司：%s\n" +
                        "📌 職稱：%s\n" +
                        "📝 分析：%s\n" +
                        "🔗 連結：%s",
                result.aiScore(),
                job.getCompanyName(),
                job.getJobTitle(),
                result.aiAnalysis(),
                job.getJobUrl()
        );

        linePushService.sendPushNotification(lineUserId, message);
        log.info(">>> [通知] 已觸發 LINE 推播");
    }
}