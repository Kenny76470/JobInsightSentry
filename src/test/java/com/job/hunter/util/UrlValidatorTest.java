package com.job.hunter.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UrlValidatorTest {

    @Test
    void testCleanUrl() {
        // 測試 1：標準網址
        assertEquals("https://www.104.com.tw/job/86xxx",
                UrlValidator.cleanUrl("https://www.104.com.tw/job/86xxx?utm_source=line"));

        // 測試 2：帶有空白
        assertEquals("https://www.104.com.tw/job/7yyy",
                UrlValidator.cleanUrl("   https://www.104.com.tw/job/7yyy   "));

        // 測試 3：垃圾網址
        assertNull(UrlValidator.cleanUrl("https://www.google.com"));

        // 測試 4：手機短網址（如果你有做處理的話）
        // assertEquals("...", UrlValidator.cleanUrl("https://104.page.link/abc"));

    }
    @Test
    void testAppShareContent() {
        // 1. 準備：這是從手機 App 複製出來的雜亂字串
        String rawInput = "我在104工作快找上發現了一個超適合你的內容，分享給你希望會喜歡~~ https://www.104.com.tw/job/8lqib?jobsource=android_share";

        // 2. 行動：呼叫你的清洗工具
        String result = UrlValidator.cleanUrl(rawInput);

        // 3. 斷言：預期它應該只剩下乾淨的網址，且參數被洗掉
        assertEquals("https://www.104.com.tw/job/8lqib", result);
    }
}