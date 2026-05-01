package com.job.hunter.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UrlValidatorTest {

    @Test
    @DisplayName("基礎網址清洗測試：包含參數洗滌、空白處理與無效網址攔截")
    void testCleanUrlBasic() {
        // 1. 測試參數洗滌 [cite: 130]
        assertEquals("https://www.104.com.tw/job/86xxx",
                UrlValidator.cleanUrl("https://www.104.com.tw/job/86xxx?utm_source=line"));

        // 2. 測試前後空白容錯 [cite: 135]
        assertEquals("https://www.104.com.tw/job/7yyy",
                UrlValidator.cleanUrl("   https://www.104.com.tw/job/7yyy   "));

        // 3. 測試非 104 網址攔截 [cite: 133]
        assertNull(UrlValidator.cleanUrl("https://www.google.com"));
    }

    @Test
    @DisplayName("進階雜訊提取測試：從 App 分享的長字串中精準提取 ID")
    void testAppShareContentExtraction() {
        // 模擬手機 App 複製出的雜亂字串 [cite: 161]
        String rawInput = "我在104工作快找上發現了一個超適合你的內容，分享給你希望會喜歡~~ " +
                "https://www.104.com.tw/job/8lqib?jobsource=android_share";

        String result = UrlValidator.cleanUrl(rawInput);

        // 驗證是否能無視雜訊鎖定標準網址格式 [cite: 162]
        assertEquals("https://www.104.com.tw/job/8lqib", result);
    }

    @Test
    @DisplayName("手機版網址支援測試：驗證 m.104.com.tw 格式")
    void testMobileSubdomainSupport() {
        // 驗證手機版子網域是否能正確標準化為官方標準頁面
        assertEquals("https://www.104.com.tw/job/8m999",
                UrlValidator.cleanUrl("https://m.104.com.tw/job/8m999"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n", "\t"})
    @DisplayName("異常輸入測試：空字串或純空白應回傳 null")
    void testInvalidBlankInputs(String input) {
        // 驗證 isBlank() 邏輯是否運作正常
        assertNull(UrlValidator.cleanUrl(input));
    }

    @Test
    @DisplayName("邊界測試：輸入 null 值不應崩潰")
    void testNullInput() {
        // 防止 NullPointerException
        assertNull(UrlValidator.cleanUrl(null));
    }
}