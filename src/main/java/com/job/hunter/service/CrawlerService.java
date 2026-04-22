package com.job.hunter.service;

import com.job.hunter.model.JobDetail;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Service;

@Service
public class CrawlerService {
    private final Browser browser;

    public CrawlerService(Browser browser) {
        this.browser = browser;
    }

    public JobDetail scrape(String url) {
        try (Page page = browser.newPage()) {
            System.out.println("【執行中】前往目標：" + url);

            // 1. 前往網址並等待
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60000));

            // 2. 等待標題出現 (確認網頁已加載)
            page.waitForSelector("h1", new Page.WaitForSelectorOptions().setTimeout(10000));

            // --- 開始精確抓取 (基於你提供的 HTML 結構) ---

            // 職稱：抓 h1
            String jobTitle = page.locator("h1").first().innerText().trim();

            // 公司名：找具有 data-gtm-head="公司名稱" 的標籤，這比 class 穩 100 倍
            String companyName = "未知公司";
            try {
                companyName = page.locator("[data-gtm-head='公司名稱']").first().innerText().trim();
            } catch (Exception e) {
                System.out.println("【警告】公司名抓取異常，改用備選方案");
            }

            // 薪資：你的 HTML 顯示它在帶有特定 class 的 p 標籤裡
            String salary = "待遇面議";
            try {
                salary = page.locator("p.text-primary.font-weight-bold").first().innerText().trim();
            } catch (Exception e) { }

            // 內容：抓 job-description__content 這個 class
            String content = "";
            try {
                content = page.locator(".job-description__content").first().innerText().trim();
            } catch (Exception e) {
                // 如果精確內容抓不到，就實施你的「暴力直覺」：抓取整個應用的文字
                content = page.locator("#app").innerText().trim();
            }

            // 條件：抓 job-requirement-table 區塊
            String condition = "請見內文";
            try {
                condition = page.locator(".job-requirement-table").first().innerText().trim();
            } catch (Exception e) { }

            // 福利：如果 HTML 沒明顯區塊，就讓 AI 從 content 裡找
            String benefit = "請見內文";

            System.out.println("【成功】抓取完成：" + jobTitle + " @ " + companyName);

            // 4. 回傳 Record (確保 6 個欄位)
            return new JobDetail(
                    companyName,
                    jobTitle,
                    salary,
                    content,
                    condition,
                    benefit
            );

        } catch (Exception e) {
            System.err.println("【錯誤】爬蟲執行失敗：" + e.getMessage());
            throw new RuntimeException("爬蟲抓取失敗: " + e.getMessage());
        }
    }
}