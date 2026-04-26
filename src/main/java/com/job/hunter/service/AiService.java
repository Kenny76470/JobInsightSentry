package com.job.hunter.service;

import com.job.hunter.model.JobDetail;
import java.io.IOException;

public interface AiService {

    /**
     * 核心分析方法
     */
    String analyze(String text) throws IOException;

    /**
     * 針對 JobDetail 物件的分析擴充
     */
    default String analyzeJob(JobDetail detail, String userKeywords) {
        try {
            // 💡 關鍵修正：Record 的方法名就是欄位名，沒有 get
            return analyze(detail.content());
        } catch (IOException e) {
            return "{}";
        }
    }
}