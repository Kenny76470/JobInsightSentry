package com.job.hunter.repository;

import com.job.hunter.model.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {
    // 繼承 JpaRepository<型態, ID型態>
    // 這裡 ID 是 String，因為我們用 job_url 當主鍵 [cite: 52, 56]

    /**
     * 💡 關鍵新增：Spring Data JPA 會自動將此方法解析為：
     * SELECT * FROM jobs WHERE ai_score IS NULL
     * 這讓 JobAnalysisService 可以批次處理待分析的職缺 [cite: 1, 172, 173]。
     */
    List<JobEntity> findTop10ByAiScoreIsNullOrderByCreatedAtDesc();
}