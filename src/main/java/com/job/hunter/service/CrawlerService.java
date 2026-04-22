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
            System.out.println("【執行中】正在開啟分頁並前往：" + url);

            // 1. 前往網址並等待
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60000));

            // 2. 等待關鍵元素出現 (確保不是空網頁)
            page.waitForSelector("h1", new Page.WaitForSelectorOptions().setTimeout(10000));

            // 3. 開始抓取 (使用更強健的組合選擇器)
            String jobTitle = page.locator("h1, .job-header__title").first().innerText().trim();

            // 抓取公司名稱 (排除掉「我有興趣」之類的按鈕文字)
            String companyName = page.locator("a[href*='company'], .company-info__nameStr").first().innerText().trim();

            // 抓取薪資 (104 的薪資通常在 .job-description-main__renderer 的第一個項目)
            String salary = "不詳";
            try {
                salary = page.locator(".job-description-main__renderer").first().innerText().split("\n")[0];
            } catch (Exception e) { /* 抓不到就用預設值 */ }

            // 抓取主內容
            String content = page.locator(".job-description-info__text").first().innerText().trim();

            // 抓取條件 (應徵條件區塊)
            String condition = "見網頁說明";
            try {
                condition = page.locator(".job-requirement-info").first().innerText().trim();
            } catch (Exception e) { }

            // 抓取福利
            String benefit = "見網頁說明";
            try {
                benefit = page.locator(".job-welfare-info").first().innerText().trim();
            } catch (Exception e) { }

            System.out.println("【成功】抓取到職缺：" + jobTitle + " @ " + companyName);

            // 4. 依照 Record 定義順序回傳 (6個參數)
            return new JobDetail(
                    companyName,  // String companyName
                    jobTitle,     // String jobTitle
                    salary,       // String salary
                    content,      // String content
                    condition,    // String condition
                    benefit       // String benefit
            );

        } catch (Exception e) {
            System.err.println("【錯誤】爬蟲執行失敗：" + e.getMessage());
            throw new RuntimeException("爬蟲抓取失敗: " + e.getMessage());
        }
    }
}