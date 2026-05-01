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

    // 定義統一的 User-Agent，強制以 PC 網頁版面渲染，避免 104 隨機給手機版
    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public CrawlerService(Browser browser) {
        this.browser = browser;
    }

    /**
     * 抓取搜尋列表 - 強力通用版
     */
    public List<String> scrapeJobList(String searchUrl) {
        log.info(">>> 【開始】抓取搜尋列表：{}", searchUrl);

        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setUserAgent(USER_AGENT));
             Page page = context.newPage()) {

            page.navigate(searchUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            try {
                page.waitForSelector(".info-job__text", new Page.WaitForSelectorOptions().setTimeout(15000));
            } catch (Exception e) {
                log.warn("⚠️ 還是抓不到職缺。DEBUG: 標題是 [{}]", page.title());
                return Collections.emptyList();
            }

            Locator links = page.locator("a.info-job__text");
            List<String> urls = new ArrayList<>();

            log.info("偵測到 {} 個潛在職缺連結", links.count());

            for (int i = 0; i < links.count(); i++) {
                String href = links.nth(i).getAttribute("href");
                if (href != null && href.contains("job/")) {
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

        // 💡 修改點 1：統一 User-Agent，讓詳情頁也保持 PC 版面，防止排版亂跳
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setUserAgent(USER_AGENT));
             Page page = context.newPage()) {

            log.info("【執行中】前往目標詳情頁：{}", url);
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            // 💡 修改點 2：不要只等 h1，必須等到「工作內容」的區塊出現才開始抓
            try {
                page.waitForSelector("p.job-description__content, div.job-description-content", new Page.WaitForSelectorOptions().setTimeout(10000));
            } catch (Exception e) {
                log.warn("等待內文元素出現超時，可能是特殊版面或網頁載入緩慢");
            }

            // 💡 修改點 3：加入 F12 分析出的多重備用選擇器 (用逗號隔開)
            String jobTitle = safeInnerText(page, "h1", "未知職稱");
            String companyName = safeInnerText(page, ".company__name, a.cmp-link, [data-gtm-head='公司名稱'], .company-name", "未知公司");
            String salary = safeInnerText(page, ".job-header__salary, .job-description-info__items p, .text-primary", "待遇面議");

            // 涵蓋標準版、手機版、企業客製版的內文選擇器
            String content = safeInnerText(page, "p.job-description__content, div.job-description-content, p.job-description-table__data", "無內文");
            String condition = safeInnerText(page, ".job-requirement-table", "請見內文");

            log.info("【成功】抓取完成：{} @ {} (內文字數: {})", jobTitle, companyName, content.length());

            JobDetail detail = new JobDetail(companyName, jobTitle, salary, content, condition, "請見內文");

            // 💡 修改點 4：把 content 傳給資料庫儲存方法
            saveJobToDatabase(url, companyName, jobTitle, content);

            return detail;

        } catch (Exception e) {
            log.error("【錯誤】詳情抓取失敗 [{}]: {}", url, e.getMessage());
            return null;
        }
    }

    private String safeInnerText(Page page, String selector, String defaultValue) {
        try {
            // first() 確保即使有多個符合的標籤，我們也只抓第一個
            Locator locator = page.locator(selector).first();
            if (locator.isVisible()) {
                return locator.innerText().trim();
            }
        } catch (Exception ignored) { }
        return defaultValue;
    }

    // 💡 修改點 5：新增 content 參數並存入 job
    private void saveJobToDatabase(String url, String company, String title, String content) {
        JobEntity job = new JobEntity();
        job.setJobUrl(url);
        job.setCompanyName(company);
        job.setJobTitle(title);
        job.setContent(content); // 👈 真正解決 null 的救星在這裡！
        jobRepository.save(job);
    }
}