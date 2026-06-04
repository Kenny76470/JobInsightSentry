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

    @Autowired
    private JobRepository jobRepository;

    // 💡 核心優化 1：使用乾淨的最新 PC 版 User-Agent，並設定大螢幕 Viewport，防止 104 判定為手機版或爬蟲
    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    /**
     * 抓取搜尋列表 - 強力破防版
     */
    public List<String> scrapeJobList(String searchUrl) {
        log.info(">>> 【開始】強力破防抓取搜尋列表：{}", searchUrl);

        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                     .setHeadless(false)// 👈 就是這行！ true 是無頭，false 是有頭
                     .setArgs(List.of(
                             "--disable-blink-features=AutomationControlled", // ❌ 抹除自動化標籤
                             "--no-sandbox",
                             "--disable-infobars",
                             "--disable-dev-shm-usage",
                             "--disable-browser-side-navigation",
                             "--disable-gpu", // 減少無頭模式下的渲染出錯
                             // 💡 關鍵破防點：手動宣告語言與網頁特徵，防止被判定為無頭機器人
                             "--lang=zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7"
                     )));
             // 💡 修正點 1：在 Context 層也塞入完整的特徵偽裝
             BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                     .setUserAgent(USER_AGENT)
                     .setViewportSize(1920, 1080)
                     .setLocale("zh-TW")
                     .setAcceptDownloads(true));
             Page page = context.newPage()) {

            // 💡 修正點 2：不要用 DOMCONTENTLOADED 了（因為骨架太早好），改回預設或直接導頁
            page.navigate(searchUrl);

            // 💡 修正點 3：給 Vue 3~5 秒的時間初始化。我們不盲目等待，直接等 104 最外層的容器或導覽列掛載完成
            log.info("正在等待 Vue 引擎掛載元件...");
            try {
                // #app 是 Vue 的掛載點，我們等它內部至少長出元件（例如 104 的搜尋吧或是主要區塊）
                page.waitForSelector("#app div, .b-block", new Page.WaitForSelectorOptions().setTimeout(6000));
            } catch (Exception e) {
                log.warn("Vue 元件掛載較慢或被阻斷，嘗試進行強行模擬...");
            }

            // 💡 修正點 4：這時候網頁有高度了，再進行動態滾動觸發 Lazy Load
            log.info("正在模擬人類向下滾動網頁以觸發 AJAX 資料載入...");
            for (int i = 0; i < 3; i++) {
                page.evaluate("window.scrollBy(0, 600);");
                page.waitForTimeout(800);
            }

            try {
                // 💡 修正點 5：最終等待職缺連結現形
                page.waitForSelector("a.info-job__text, div.b-block__left a[href*='job/']",
                        new Page.WaitForSelectorOptions().setTimeout(10000));
            } catch (Exception e) {
                String bodyHtml = page.locator("body").innerHTML();
                log.warn("⚠️ 依舊超時！104 拒絕渲染職缺列表。DEBUG HTML 關鍵片段:\n{}",
                        bodyHtml.substring(0, Math.min(bodyHtml.length(), 1000)));
                return Collections.emptyList();
            }

            // 抓取連結
            Locator links = page.locator("a.info-job__text, div.b-block__left a[href*='job/']");
            List<String> urls = new ArrayList<>();
            int count = links.count();
            log.info("偵測到 {} 個潛在職缺連結", count);

            for (int i = 0; i < count; i++) {
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

            log.info("<<< 【完成】列表抓取，共過濾出 {} 個不重複職缺 URL", urls.size());
            return urls.stream().distinct().toList();

        } catch (Exception e) {
            log.error("【嚴重錯誤】列表抓取期間發生非預期崩潰：", e);
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

        // 💡 核心優化 6：詳情頁同步採用獨立生命週期管理，全面阻斷資源洩漏
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                     .setHeadless(true)
                     .setArgs(List.of("--disable-blink-features=AutomationControlled")));
             BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                     .setUserAgent(USER_AGENT)
                     .setViewportSize(1920, 1080));
             Page page = context.newPage()) {

            log.info("【執行中】前往目標詳情頁：{}", url);
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            try {
                page.waitForSelector("p.job-description__content, div.job-description-content",
                        new Page.WaitForSelectorOptions().setTimeout(10000));
            } catch (Exception e) {
                log.warn("等待內文元素出現超時，可能是特殊版面或網頁載入緩慢");
            }

            String jobTitle = safeInnerText(page, "h1", "未知職稱");
            String companyName = safeInnerText(page, ".company__name, a.cmp-link, [data-gtm-head='公司名稱'], .company-name", "未知公司");
            String salary = safeInnerText(page, ".job-header__salary, .job-description-info__items p, .text-primary", "待遇面議");
            String content = safeInnerText(page, "p.job-description__content, div.job-description-content, p.job-description-table__data", "無內文");
            String condition = safeInnerText(page, ".job-requirement-table", "請見內文");

            log.info("【成功】抓取完成：{} @ {} (內文字數: {})", jobTitle, companyName, content.length());

            JobDetail detail = new JobDetail(companyName, jobTitle, salary, content, condition, "請見內文");

            // 💡 核心優化 7：防禦性寫入。多執行緒排程高併發時，若遇到同秒寫入，捕獲衝突異常，防止排程炸開
            try {
                saveJobToDatabase(url, companyName, jobTitle, content);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                log.warn("⚠️ 寫入資料庫時發生主鍵衝突（其他執行緒可能已先一步寫入）：{}", url);
            }

            return detail;

        } catch (Exception e) {
            log.error("【錯誤】詳情抓取失敗 [{}]: ", url, e);
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

    private void saveJobToDatabase(String url, String company, String title, String content) {
        JobEntity job = new JobEntity();
        job.setJobUrl(url);
        job.setCompanyName(company);
        job.setJobTitle(title);
        job.setContent(content);
        jobRepository.save(job);
    }
}