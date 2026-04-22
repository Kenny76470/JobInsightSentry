好的，這是一個非常專業的決策。在工程領域，我們稱之為 **「專案管理與技術債清單」**。

為了讓你從目前的 Java SE 順利過渡到 Spring Boot，並且補齊資安與測試的漏洞，我幫你列了一份 **「JobInsightSentry 升級藍圖」**。你可以直接把這段存進你的 `TODO.md` 檔案裡。

---

### 📋 JobInsightSentry 開發待辦清單 (TODO List)

#### 第一階段：穩固地基 (Java SE 最終修正) —— **今天目標**
* [x] **資安修補**：在根目錄建立 `.env`，將 API Key 從 `GeminiService` 拔掉，改用 `dotenv` 讀取。
* [x] **Git 存檔**：
    * [x] 撰寫完整的 `.gitignore`（排除 `.idea`, `target`, `.env`, `JobExports`）。
    * [x] 執行 `git init` 並完成第一次 Commit：`feat: baseline java-se version`。
* [x] **測試補全**：
    * [x] 跑通 `UrlValidatorTest` 的所有案例（包含你剛找的 104 App 分享網址）。
    * [x] 撰寫 `ContentFilterTest`，驗證黑名單 Regex 是否能擋掉「博弈、保險」。

#### 第二階段：架構變身 (Spring Boot 遷移) —— **核心改版**
* [ ] **環境換血**：修改 `pom.xml`，加入 `spring-boot-starter-parent` 與 `web-starter`。
* [ ] **建立啟動類**：建立 `JobHunterApplication.java` 並加上 `@SpringBootApplication`。
* [ ] **元件化 (Bean)**：
    * [ ] 將 `GeminiService` 加上 `@Service`。
    * [ ] 將 `CrawlerService` 改為由 Spring 管理。
* [ ] **入口開發 (Controller)**：建立 `JobController`，提供 `POST /api/analyze` 介面，取代 CLI 輸入。

#### 第三階段：產品優化 (專業工程化) —— **面試加分項**
* [ ] **異常處理 (Global Exception Handling)**：建立 `@ControllerAdvice`，確保爬蟲失敗時回傳漂亮的 JSON 而不是一堆紅字。
* [ ] **日誌系統 (Logging)**：把 `System.out.println` 全部換成 `SLF4J + Logback`。
* [ ] **資料持久化**：
    * [ ] 引入 **H2 Database**（測試用）或 **MySQL**。
    * [ ] 使用 Spring Data JPA 將分析結果存入資料庫，取代 `.txt` 存檔。

---

### 💡 建議的操作流程

你可以先把這份清單貼到 IntelliJ 的一個新檔案 `TODO.md`。每完成一項，就在 `[ ]` 裡填入 `x` 變成 `[x]`。

**現在，我們來處理「第一階段」的第一個魔王：Git。**

1.  你在專案根目錄（跟 `src` 同一層）建立一個 `TODO.md` 了嗎？
2.  建立完後，我教你寫那個專業的 `.gitignore` 檔案，這才是目前最迫切的「資安防護」，免得你一上傳 GitHub 就洩漏金鑰。



**如果你準備好了，跟我說一聲，我們來寫 `.gitignore` 並執行 `git init`！**