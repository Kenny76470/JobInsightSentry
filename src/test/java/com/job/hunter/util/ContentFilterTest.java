package com.job.hunter.util;

import com.job.hunter.model.JobDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentFilterTest {

    @Test
    @DisplayName("應該過濾掉標題包含黑名單關鍵字的職缺")
    void shouldFilterBlacklistedJobs() {
        // 順序：公司, 標題, 薪水, 內容, 條件, 福利
        JobDetail insuranceJob = new JobDetail("某某人壽", "高薪保險理財專員", "面議", "這是一份保險工作內容...", "無", "無");
        JobDetail gamblingJob = new JobDetail("海外公司", "博弈後端工程師", "高薪", "維護洗碼系統...", "無", "無");

        assertTrue(ContentFilter.isGarbage(insuranceJob), "保險職缺應該被擋掉");
        assertTrue(ContentFilter.isGarbage(gamblingJob), "博弈職缺應該被擋掉");
    }

    @Test
    @DisplayName("不應該過濾掉正常的技術職缺")
    void shouldNotFilterNormalJobs() {
        String longContent = "這是一段超過五十個字的內容描述，為了確保測試能夠通過過濾器的長度檢查。".repeat(2);

        // 內容 (longContent) 必須放在第 4 個參數
        JobDetail javaJob = new JobDetail("國泰世華", "Java 工程師", "45k-65k", longContent, "Java 經驗", "年終");
        JobDetail tsmcJob = new JobDetail("台積電", "IT 軟體工程師", "面議", longContent, "碩士畢業", "分紅");

        assertFalse(ContentFilter.isGarbage(javaJob), "正常的 Java 職缺不應被擋掉");
        assertFalse(ContentFilter.isGarbage(tsmcJob), "正常的 TSMC 職缺不應被擋掉");
    }

    @Test
    @DisplayName("應該過濾掉內容描述過短的職缺")
    void shouldFilterShortContentJobs() {
        // 內容 "來就對了" 放在第 4 個參數，長度為 4
        JobDetail shadyJob = new JobDetail("神祕公司", "誠徵工程師", "面議", "來就對了", "無", "無");

        assertTrue(ContentFilter.isGarbage(shadyJob), "內容極短的職缺應被視為垃圾");
    }
}