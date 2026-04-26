package com.job.hunter.model;

/**
 * 使用 Record 語法，編譯器會自動生成 aiScore() 和 aiAnalysis() 方法
 */
public record AiAnalysisResult(int aiScore, String aiAnalysis) {}