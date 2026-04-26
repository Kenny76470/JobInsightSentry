package com.job.hunter.service;

import com.job.hunter.model.JobDetail;
import com.job.hunter.model.JobEntity;
import com.job.hunter.repository.JobRepository;
import com.job.hunter.util.UrlValidator;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class CrawlerService {
    private final Browser browser;

    @Autowired
    private JobRepository jobRepository;

    public CrawlerService(Browser browser) {
        this.browser = browser;
    }

    /**
     * 抓取搜尋列表 - 強力通用版
     */
    public List<String> scrapeJobList(String searchUrl) {
        log.info(">>> 【開始】抓取搜尋列表：{}", searchUrl);

        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"));
             Page page = context.newPage()) {

            // 1. 前往頁面，等到網路靜止 (Vue 渲染需要時間)
            page.navigate(searchUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            // 2. 💡 修正後的定位器：根據你提供的 HTML，職缺標題連結帶有 info-job__text
            try {
                page.waitForSelector(".info-job__text", new Page.WaitForSelectorOptions().setTimeout(15000));
            } catch (Exception e) {
                log.warn("⚠️ 還是抓不到職缺。DEBUG: 標題是 [{}]", page.title());
                return Collections.emptyList();
            }

            // 3. 💡 深度採集：抓取所有帶有 jobsource 參數的連結，這是 104 職缺的特徵
            // 或者直接鎖定 info-job__text
            Locator links = page.locator("a.info-job__text");
            List<String> urls = new ArrayList<>();

            log.info("偵測到 {} 個潛在職缺連結", links.count());

            for (int i = 0; i < links.count(); i++) {
                String href = links.nth(i).getAttribute("href");
                if (href != null && href.contains("job/")) {
                    // 補全 URL
                    String fullUrl = href.startsWith("//") ? "https:" + href : href;
                    fullUrl = fullUrl.startsWith("/") ? "https://www.104.com.tw" + fullUrl : fullUrl;

                    String cleanUrl = UrlValidator.clean(fullUrl);
                    if (cleanUrl != null) {
                        urls.add(cleanUrl);
                    }
                }
            }

            log.info("<<< 【完成】列表抓取，共發現 {} 個職缺 URL", urls.size());
            return urls.stream().distinct().toList();

        } catch (Exception e) {
            log.error("【嚴重錯誤】列表抓取失敗：{}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 抓取單個職缺詳情
     */
    public JobDetail scrape(String url) {
        if (jobRepository.existsById(url)) {
            log.info(">>> 【跳過】資料庫已有紀錄：{}", url);
            return null;
        }

        try (BrowserContext context = browser.newContext();
             Page page = context.newPage()) {

            log.info("【執行中】前往目標詳情頁：{}", url);
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            page.waitForSelector("h1", new Page.WaitForSelectorOptions().setTimeout(10000));

            String jobTitle = safeInnerText(page, "h1", "未知職稱");
            String companyName = safeInnerText(page, "[data-gtm-head='公司名稱'], .company-name", "未知公司");
            String salary = safeInnerText(page, ".job-description-info__items p, .text-primary", "待遇面議");

            String content = safeInnerText(page, ".job-description__content", "無內文");
            if ("無內文".equals(content)) {
                content = safeInnerText(page, ".job-description", "內容解析失敗");
            }

            String condition = safeInnerText(page, ".job-requirement-table", "請見內文");

            log.info("【成功】抓取完成：{} @ {}", jobTitle, companyName);

            JobDetail detail = new JobDetail(companyName, jobTitle, salary, content, condition, "請見內文");
            saveJobToDatabase(url, companyName, jobTitle);

            return detail;

        } catch (Exception e) {
            log.error("【錯誤】詳情抓取失敗 [{}]: {}", url, e.getMessage());
            return null;
        }
    }

    private String safeInnerText(Page page, String selector, String defaultValue) {
        try {
            Locator locator = page.locator(selector).first();
            if (locator.isVisible()) {
                return locator.innerText().trim();
            }
        } catch (Exception ignored) { }
        return defaultValue;
    }

    private void saveJobToDatabase(String url, String company, String title) {
        JobEntity job = new JobEntity();
        job.setJobUrl(url);
        job.setCompanyName(company);
        job.setJobTitle(title);
        jobRepository.save(job);
    }
}