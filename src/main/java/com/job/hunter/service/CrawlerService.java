package com.job.hunter.service;

import com.job.hunter.model.JobDetail;
import com.job.hunter.model.JobEntity;
import com.job.hunter.repository.JobRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CrawlerService {
    private final Browser browser;

    @Autowired
    private JobRepository jobRepository; // 注入守門員

    public CrawlerService(Browser browser) {
        this.browser = browser;
    }

    public JobDetail scrape(String url) {
        // 【核心邏輯：查重】在發起昂貴的爬蟲與 AI 請求前，先問資料庫
        if (jobRepository.existsById(url)) {
            System.out.println(">>> 【跳過】資料庫已有紀錄，不再重複爬取：" + url);
            // 實務上這裡可以回傳 null 或從資料庫抓舊資料，目前我們先用 skip 概念
            return null;
        }

        try (Page page = browser.newPage()) {
            System.out.println("【執行中】前往目標新職缺：" + url);

            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60000));

            page.waitForSelector("h1", new Page.WaitForSelectorOptions().setTimeout(10000));

            // --- 開始精確抓取 ---
            String jobTitle = page.locator("h1").first().innerText().trim();

            String companyName = "未知公司";
            try {
                companyName = page.locator("[data-gtm-head='公司名稱']").first().innerText().trim();
            } catch (Exception e) {
                System.out.println("【警告】公司名抓取異常");
            }

            String salary = "待遇面議";
            try {
                salary = page.locator("p.text-primary.font-weight-bold").first().innerText().trim();
            } catch (Exception e) { }

            String content = "";
            try {
                content = page.locator(".job-description__content").first().innerText().trim();
            } catch (Exception e) {
                content = page.locator("#app").innerText().trim();
            }

            String condition = "請見內文";
            try {
                condition = page.locator(".job-requirement-table").first().innerText().trim();
            } catch (Exception e) { }

            String benefit = "請見內文";

            System.out.println("【成功】抓取完成：" + jobTitle + " @ " + companyName);

            // --- 準備封裝回傳內容 ---
            JobDetail detail = new JobDetail(
                    companyName,
                    jobTitle,
                    salary,
                    content,
                    condition,
                    benefit
            );

            // 【重要】暫時存入資料庫的基本資訊，確保下次不會重複爬
            // 等你稍後實作 AI 分析後，我們會把 aiScore 也存進去
            saveJobToDatabase(url, companyName, jobTitle);

            return detail;

        } catch (Exception e) {
            System.err.println("【錯誤】爬蟲執行失敗：" + e.getMessage());
            throw new RuntimeException("爬蟲抓取失敗: " + e.getMessage());
        }
    }

    // 輔助方法：將抓到的基本資訊存入 PostgreSQL
    private void saveJobToDatabase(String url, String company, String title) {
        JobEntity job = new JobEntity();
        job.setJobUrl(url);
        job.setCompanyName(company);
        job.setJobTitle(title);
        // aiAnalysis 和 aiScore 可以在之後 AI 分析完再 update
        jobRepository.save(job);
        System.out.println(">>> 【存檔】職缺已紀錄至資料庫，下次將自動跳過。");
    }
}