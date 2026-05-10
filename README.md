

# JobInsightSentry - AI 職缺追蹤與自動化分析系統

JobInsightSentry 是一個基於 **Spring Boot** 開發的自動化求職助手。它結合了 **Playwright 網頁爬蟲**、**LLM (Gemini/Ollama) AI 分析** 與 **LINE Messaging API**，能自動從 104 等求職平台擷取職缺、利用 AI 進行適配度分析，並將結果即時推送至使用者手機。

## 🚀 核心功能

* **自動化動態爬蟲**：使用 **Microsoft Playwright** 模擬瀏覽器行為，有效處理 SPA 動態渲染網頁，突破傳統 HTTP 請求的限制。
* **多模態 AI 分析系統**：整合 **Google Gemini API** 與 **Ollama (本地端模型)**，支援職缺內容摘要、關鍵技能提取及適配度評分。
* **智能資料清洗**：針對 LLM 輸出的非格式化文字，實作**自研大括號定位萃取演算法**，確保 JSON 解析成功率達 99% 以上。
* **高可靠性排程與限流**：內建動態冷卻機制與**非同步補考佇列 (Retry Mechanism)**，自動應對第三方 API 的 Rate Limit (HTTP 429/503)。
* **即時推播通知**：整合 LINE Messaging API，針對符合條件的優質職缺進行主動通知。

## 🛠️ 技術棧

* **後端框架**：Java 21 / Spring Boot 3.x
* **網頁爬蟲**：Playwright
* **AI 整合**：Gemini API / Ollama (Qwen 2.5)
* **資料庫**：PostgreSQL / Spring Data JPA
* **連線工具**：OkHttp3 / Gson
* **環境管理**：dotenv-java

## 🏗️ 系統亮點與架構設計

### 1. 策略模式 (Strategy Pattern) 實現模型解耦
系統將 AI 邏輯抽象化為 `AiService` 介面，利用 Spring 的 `@Primary` 註解實現 Gemini (雲端) 與 Ollama (本地) 的無縫切換。這不僅降低了 API 成本，也提升了系統在不同環境下的適應性。

### 2. 防禦性編程 (Defensive Programming)
在 `JobAnalysisService` 中實作多層資料檢核（Null/Blank Check），並針對網路不穩或 AI 幻覺問題設計了健壯的例外處理機制。

### 3. 高可用性排程管理
在 `JobScheduler` 中設計「成功休息 10 秒 / 失敗休息 60 秒」的動態冷卻邏輯，有效保護 IP 避免被目標網站封鎖。

## ⚙️ 環境配置

請在專案根目錄建立 `.env` 檔案，並配置以下參數：

```env
# 資料庫設定
DB_USERNAME=your_username
DB_PASSWORD=your_password

# AI 憑證
GEMINI_API_KEY=your_gemini_key

# LINE 推播設定
LINE_CHANNEL_ACCESS_TOKEN=your_token
LINE_USER_ID=your_id
```

## 📖 如何啟動

**1. Clone 專案：**

```bash
git clone [https://github.com/Kenny76470/JobInsightSentry.git](https://github.com/Kenny76470/JobInsightSentry.git)
```

**2. 配置環境：**
依上述 `.env` 範例，在專案根目錄建立 `.env` 檔案並填寫您的憑證。

**3. 編譯並執行：**

```bash
./mvnw spring-boot:run

```

---

*本專案為求職作品集，展示了後端架構設計、第三方 API 整合及自動化系統開發能力。*
