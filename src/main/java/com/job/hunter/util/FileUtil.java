package com.job.hunter.util;

import com.job.hunter.model.JobDetail;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

public class FileUtil {

    private static final String EXPORT_DIR = "JobExports";

    // 1. 原本的存職缺方法
    public static void saveJob(JobDetail job, String url) {
        try {
            ensureDirectoryExists();
            String fileName = getCleanFileName(job) + ".txt";
            File file = new File(EXPORT_DIR, fileName);

            try (PrintWriter writer = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                writer.println("抓取時間：" + LocalDateTime.now());
                writer.println("抓取網址：" + url);
                writer.println("公司：" + job.companyName());
                writer.println("職稱：" + job.jobTitle());
                writer.println("--------------------------------------------------");
                writer.println("\n工作內容：\n" + job.content());
                writer.println("\n條件要求：\n" + job.condition());
                writer.println("\n福利：\n" + job.benefit());
                System.out.println("💾 職缺存檔成功：" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("❌ 存檔失敗: " + e.getMessage());
        }
    }

    // 2. 補上你 Main 裡面缺少的 saveAnalysis 方法
    public static void saveAnalysis(JobDetail job, String analysis) {
        try {
            ensureDirectoryExists();
            // 加個後綴區分是 AI 分析報告
            String fileName = getCleanFileName(job) + "_AI分析.txt";
            File file = new File(EXPORT_DIR, fileName);

            try (PrintWriter writer = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                writer.println("=== Gemini AI 職缺鑑定報告 ===");
                writer.println("分析時間：" + LocalDateTime.now());
                writer.println("目標公司：" + job.companyName());
                writer.println("目標職稱：" + job.jobTitle());
                writer.println("--------------------------------------------------");
                writer.println(analysis);
                System.out.println("📝 AI 分析報告已存檔：" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("❌ AI 報告存檔失敗: " + e.getMessage());
        }
    }

    // 輔助工具：確保資料夾存在
    private static void ensureDirectoryExists() throws Exception {
        Path path = Paths.get(EXPORT_DIR);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    // 輔助工具：統一清洗檔名邏輯 (SRP 體現)
    private static String getCleanFileName(JobDetail job) {
        return (job.companyName() + "_" + job.jobTitle()).replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}