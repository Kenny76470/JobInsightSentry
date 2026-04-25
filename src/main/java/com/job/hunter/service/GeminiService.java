package com.job.hunter.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit; // 👈 記得導入這個

@Service
@Profile("prod")
public class GeminiService implements AiService {

    private final OkHttpClient client;
    private final String apiKey;
    private final Gson gson = new Gson();

    // 改為不依賴外部注入，直接在內部構建長超時的 Client
    public GeminiService() {
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("GEMINI_API_KEY");

        // 🚀 針對 Free Tier 調整：連線與讀取都給足 60 秒
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true) // 網路不穩時自動重試
                .build();
    }

    @Override
    public String analyze(String text) throws IOException {
        // 1. 防呆：避免文字過長
        String truncatedText = text.length() > 10000 ? text.substring(0, 10000) : text;

        // 2. 構建 URL (使用確認存在的 2.5-flash)
        HttpUrl url = HttpUrl.parse("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
                .newBuilder()
                .addQueryParameter("key", apiKey)
                .build();

        // 3. 結構化 Prompt
        String prompt = String.format("""
                你是一位資深的職涯顧問。以下是從招聘網站抓取的原始文字流。
                請忽略廣告，針對該職缺進行：
                1. 職缺亮點與缺點分析
                2. 建議避雷點（特別注意薪資面議、加班隱喻等）
                3. 面試準備建議
                
                職缺原始內容如下：
                ---
                %s
                """, truncatedText);

        // 4. 設定 GenerationConfig 讓回應更穩定 (Free Tier 建議加上)
        Map<String, Object> requestMap = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 4096
                )
        );

        String json = gson.toJson(requestMap);
        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // 5. 執行請求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown Error";
                // 如果是 429，代表 Free Tier 頻率太高了
                if (response.code() == 429) {
                    return "❌ AI 忙碌中 (429) - 免費版限制每分鐘請求次數，請稍等一分鐘再試。";
                }
                return "❌ AI 分析失敗 (Status: " + response.code() + ") - " + errorBody;
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return extractTextFromResponse(responseBody);
        }
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();

                // 檢查是否被安全過濾器攔截 (Free Tier 常見)
                if (firstCandidate.has("finishReason") &&
                        firstCandidate.get("finishReason").getAsString().equals("SAFETY")) {
                    return "⚠️ 內容被 AI 安全機制攔截，無法產生分析報告。";
                }

                JsonObject content = firstCandidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    return parts.get(0).getAsJsonObject().get("text").getAsString();
                }
            }
            return "⚠️ 無法解析 AI 回傳內容，原始資料：" + responseBody;
        } catch (Exception e) {
            return "⚠️ 解析 JSON 時出錯：" + e.getMessage();
        }
    }
}