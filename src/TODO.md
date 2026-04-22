根據你目前的專案進度與程式碼結構，身為資深開發者，我建議你按照以下優先順序完成 **TODO List**。這份清單分為「功能修補」、「系統健壯性」與「進階優化」三個階段：

### 🏁 第一階段：功能修補 (把斷掉的鏈結補起來)
目前程式碼中有些工具類寫好了但沒用到，或是關鍵設定還沒處理。

* [x] **API Key 安全化**：將 `GeminiService` 中的 `apiKey` 移至 `.env` 檔案，並使用你已引入的 `dotenv-java` 讀取，避免 API Key 意外上傳到 GitHub。
* [x] **補齊存檔邏輯**：在 `JobController` 的 `scanJob` 方法末尾，呼叫 `FileUtil.saveJob()` 與 `FileUtil.saveAnalysis()`，確保抓取的結果會存入 `JobExports` 資料夾。
* [x] **URL 預處理**：在 `JobController` 接收到 `url` 參數時，先過濾 `UrlValidator.cleanUrl(url)`，防止帶有追蹤參數的網址導致爬蟲行為異常。
* [x] **處理 JSON 轉義問題**：將 `GeminiService` 手動拼接 JSON 的方式改用 `Gson` 轉換，避免職缺描述內容包含引號時導致 API 請求失敗。

### 🛡️ 第二階段：系統健壯性 (防止程式崩潰)
爬蟲與 AI 調用是最容易出錯的地方，需要加強異常處理。

* [ ] **爬蟲超時與重試機制**：104 有時會擋爬蟲或讀取過久，在 `CrawlerService` 增加 `page.waitForLoadState()` 或適當的重試邏輯。
* [ ] **空值檢查 (Null Safety)**：在 `ContentFilter` 與 `FileUtil` 中增加對 `JobDetail` 欄位的空值檢查，防止 `NullPointerException`。
* [ ] **自定義異常處理**：建立一個 `@RestControllerAdvice` 來統一處理 `scanJob` 拋出的錯誤，不要直接把 `StackTrace` 回傳給前端。
* [ ] **Headless 模式切換**：在 `BrowserConfig` 增加一個配置項，讓你能在開發時看瀏覽器動（`headless: false`），部署到伺服器時自動關閉（`headless: true`）。

### 🚀 第三階段：進階優化 (資深工程師的追求)
讓專案從「可以用」變成「好用」且「專業」。

* [ ] **非同步處理 (Async)**：抓取與分析可能耗時超過 30 秒，考慮將 API 改為非同步，先回傳「受理中」，處理完後透過 WebSocket 通知或讓使用者查詢狀態。
* [ ] **爬蟲 CSS Selector 抽離**：將 104 的網頁選擇器（如 `h1`, `.company-info__name`）移至 `application.properties`，若網頁改版只需改設定檔，不需重新編譯。
* [ ] **優化 AI Prompt**：調整 `GeminiService` 的 Prompt，要求 AI 以固定格式（如 Markdown 表格）回傳，方便閱讀與後續資料提取。
* [ ] **多平台支援預留**：重構 `CrawlerService` 介面，讓未來除了 104 以外，也能輕鬆擴充支援 CakeResume 或 LinkedIn。

---

**你可以先把這份清單貼進專案根目錄的 `TODO.md`，完成一項打一個勾！** 有哪一部分需要我幫你寫範例程式碼嗎？