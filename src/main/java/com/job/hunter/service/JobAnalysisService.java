package com.job.hunter.service;

import com.google.gson.Gson;
import com.job.hunter.model.AiAnalysisResult;
import com.job.hunter.model.JobEntity;
import com.job.hunter.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JobAnalysisService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AiService aiService;

    private final Gson gson = new Gson();

    @Transactional
    public void analyzeAndBackfill(String url) {
        JobEntity job = jobRepository.findById(url).orElse(null);
        if (job == null) return;

        log.info(">>> [AI 分析開始] 正在分析職缺：{}", job.getJobTitle());

        try {
            String prompt = String.format(
                    "請分析職缺：【%s @ %s】。請嚴格以 JSON 格式回傳：{\"aiScore\": 分數, \"aiAnalysis\": \"評價\"}",
                    job.getJobTitle(), job.getCompanyName()
            );

            String jsonResponse = aiService.analyze(prompt);

            AiAnalysisResult result = gson.fromJson(jsonResponse, AiAnalysisResult.class);

            // 💡 修正 1：如果你用 Record，這裡的方法名是 aiScore() 和 aiAnalysis()
            job.setAiScore(result.aiScore());
            job.setAiAnalysis(result.aiAnalysis());

            jobRepository.save(job);

            // 💡 修正 2：AiScore 必須小寫 a，變成 aiScore()
            log.info(">>> [成功] 職缺分析已更新，得分：{}", result.aiScore());

        } catch (Exception e) {
            log.error(">>> [失敗] AI 分析流程異常: {}", e.getMessage());
            job.setAiScore(-1);
            jobRepository.save(job);
        }
    }
}