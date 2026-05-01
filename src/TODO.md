# 🧪 Unit Test 開發計畫

## ✅ 基礎工具組件 (Utilities)
- [x] **UrlValidatorTest**：完成基礎清洗、App 分享字串提取與手機版子網域支援測試。
- [x] **ContentFilterTest**：
    - [x] 撰寫黑名單關鍵字（博弈、保險）的正則表達式攔截測試。
    - [x] 撰寫「內容過短」職缺的自動過濾測試。
    - [x] (進階) 模擬 Mock 資料庫讀取黑名單，驗證動態過濾邏輯。
- [ ] **FileUtilTest**：
    - [ ] 測試檔名合法化邏輯（getCleanFileName），確保特殊字元會被替換為底線。
    - [ ] 驗證導出目錄 `JobExports` 是否會自動建立。

## 🤖 AI 與 業務邏輯 (Services)
- [ ] **GeminiServiceTest**：
    - [ ] 撰寫 AI 原始回傳解析測試，模擬移除 ```json``` 標籤的穩定性。
    - [ ] 模擬 API 解析失敗時，驗證是否回傳預設的錯誤 JSON 結構。
- [ ] **JobAnalysisServiceTest**：
    - [ ] 測試分數門檻（aiScore >= 80）是否正確觸發推播邏輯。
    - [ ] 模擬 API 429 錯誤發生時，驗證 `analyzeAllPendingJobs` 是否能正確中斷循環。
    - [ ] 驗證重複職缺（existsById）是否能被正確跳過以節省配額。

## 🕸️ 採集模組 (Scraper)
- [ ] **CrawlerServiceTest**：
    - [ ] 利用 Mockito 模擬 Playwright 的 Page 物件，測試詳情頁欄位（公司名、職稱）的抓取邏輯。
    - [ ] 驗證當 `safeInnerText` 找不到元素時，是否正確給予「待遇面議」等預設值。