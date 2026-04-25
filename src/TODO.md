這是一份為你的專案量身打造的 `README_TODO.md`。你可以直接將以下內容存檔，方便你在 Git 進行版本管理時同步追蹤進度。

---

# 📝 JobInsightSentry 開發清單 (TODO List)

> **專案狀態**：`Phase 1: 核心功能驗證已完成`
> **當前版本**：v0.1.0-alpha

---

## 🟥 P0: 系統穩定性與資源管理 (優先處理)
- [ ] **Browser 生命週期管理**：在 `BrowserConfig` 加入 `@PreDestroy`，確保伺服器關閉時 Playwright 資源完全釋放。
- [ ] **Context 隔離機制**：重構 `CrawlerService`，為每個 `scrape` 請求建立獨立的 `BrowserContext`（解決 Session 衝突風險）。
- [ ] **配置標準化**：將 `.env` 中的 `GEMINI_API_KEY` 整合進 Spring `application.properties`，統一管理環境變數。
- [ ] **錯誤重試機制**：針對 104 頻繁更新導致的 `TimeoutException` 加入簡易的 Retry 邏輯。

## 🟧 P1: 爬蟲效能與 AI 精準度優化
- [ ] **資源過濾 (Ad-Block)**：設定 Playwright 路由攔截，禁止加載 `image`、`font`、`stylesheet` (選用)，提升 2x 以上爬取速度。
- [ ] **文本精簡 (Token Saver)**：開發 `ContentCleaner` 工具類，過濾掉「我要應徵、檢舉職缺、相似職缺」等雜訊，節省 Gemini Token 消耗。
- [ ] **JSON 解析重構**：建立 `GeminiResponse` POJO 類，取代目前手動 `get(0).getAsJsonObject()` 的寫法。
- [ ] **Headless 模式切換**：新增配置參數，支持開發環境 (Headless: false) 與生產環境 (Headless: true) 切換。

## 🟩 P2: 功能擴充與使用者體驗
- [ ] **Git 安全檢查**：確認 `.gitignore` 已正確排除 `.env`、`target/`、`.idea/` 及 `JobExports/` 資料夾。
- [ ] **多站點抽象化**：定義 `JobCrawler` 介面，為未來接入 **CakeResume** 與 **LinkedIn** 做準備。
- [ ] **存檔格式多樣化**：除了 `.txt`，增加輸出 `.json` 或 `.html` 格式，方便後端處理或瀏覽器查看。
- [ ] **URL 寬容度優化**：優化 `UrlValidator`，支援 104 手機版網址 (`m.104.com.tw`)。

## 🟦 P3: 長期架構演進
- [ ] **非同步架構 (@Async)**：引入 Spring Task Executor，將爬蟲與 AI 分析改為異步執行，避免阻塞 REST API。
- [ ] **數據庫持久化**：從檔案系統遷移至 **H2** 或 **SQLite**，以便後續進行職缺數據分析。
- [ ] **Web 儀表板**：開發簡單的 Thymeleaf 或 React 介面，提供歷史鑑定報告的列表與關鍵字搜尋。

---

## 📈 目前 Workflow 分析摘要

1. **Input**: `JobController` 接收網址。
2. **Clean**: `UrlValidator` 規整網址。
3. **Crawl**: `CrawlerService` 透過 Playwright 抓取。 (🚨 *待優化：目前為共用 Context*)
4. **Filter**: `ContentFilter` 攔截黑名單職缺。
5. **AI**: `GeminiService` 進行專業職涯分析。
6. **Save**: `FileUtil` 產出實體報告。
7. **Output**: 回傳結構化字串。

---
**最後更新日期**：2026-04-23  
**負責人**：職缺分析哨兵開發組