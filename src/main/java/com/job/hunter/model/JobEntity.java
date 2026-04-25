package com.job.hunter.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs") // 資料庫裡的資料表名稱
public class JobEntity {

    @Id
    @Column(name = "job_url", nullable = false, unique = true)
    private String jobUrl; // 使用 URL 作為 Primary Key，確保同一個職缺不會被重複存入

    private String companyName;
    private String jobTitle;

    @Column(columnDefinition = "TEXT") // 職缺內容通常很長，所以用 TEXT 型態
    private String aiAnalysis;

    private int aiScore;

    private LocalDateTime createdAt;

    // 啟動時自動設定建立時間
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- 以下是 Getter 與 Setter (資深工程師建議你可以用 Lombok 省略這段，但我們先手寫確保你懂) ---

    public String getJobUrl() { return jobUrl; }
    public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }

    public int getAiScore() { return aiScore; }
    public void setAiScore(int aiScore) { this.aiScore = aiScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}