package com.job.hunter.service;

import com.job.hunter.model.JobDetail;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

public class CrawlerService {
    private final Page page;

    public CrawlerService(Page page) {
        this.page = page;
    }

    public JobDetail scrape(String url) {
        System.out.println("【執行中】前往目標頁面...");
        page.setDefaultTimeout(20000);
        page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        page.waitForSelector("h1", new Page.WaitForSelectorOptions().setTimeout(10000));
        page.mouse().wheel(0, 1500);
        page.waitForTimeout(3000);

        return new JobDetail(
                safeGetText(".company-info__name, .b-title, [data-qa-id='jobDetailCompanyName']", "未知公司"),
                safeGetText("h1", "未知職稱"),
                safeGetText(".job-description__content .text-primary, .tag-item.text-primary", "面議或未標示"),
                safeGetText(".job-description__content, #job-description", "無法讀取內容"),
                safeGetText(".job-requirement-table, .job-requirement", "無法讀取條件"),
                safeGetText(".job-benefit, [class*='benefit']", "未標示福利")
        );
    }

    private String safeGetText(String selector, String defaultValue) {
        try {
            Locator locator = page.locator(selector).first();
            if (locator.isVisible()) return locator.innerText().trim();
        } catch (Exception ignored) {}
        return defaultValue;
    }
}