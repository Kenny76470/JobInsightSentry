package com.job.hunter.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final OkHttpClient client;
    private final String apiKey;
    private final Gson gson = new Gson();

    public GeminiService(OkHttpClient client) {
        // 載入 .env 檔案
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("GEMINI_API_KEY");
        this.client = client;
    }

    public String analyze(String text) throws IOException {
        // 1. 防呆：避免文字過長導致 API 報錯 (1.5 Flash 支援很長，但我們先截取精華部分)
        String truncatedText = text.length() > 20000 ? text.substring(0, 20000) : text;

        // 改用最新的 3.1 Flash Lite，速度最快
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=" + apiKey;

        // 3. 結構化 Prompt，讓 AI 在面對「暴力文字流」時表現更穩定
        String prompt = String.format("""
                你是一位資深的職涯顧問。以下是從招聘網站抓取的原始文字流。
                請先忽略裡面的廣告與無關資訊，針對該職缺進行：
                1. 職缺亮點與缺點分析
                2. 建議避雷點（特別注意薪資面議、加班隱喻等）
                3. 面試準備與轉職建議
                
                職缺原始內容如下：
                ---
                %s
                """, truncatedText);

        Map<String, Object> requestMap = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String json = gson.toJson(requestMap);

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown Error";
                return "❌ AI 分析失敗 (Status: " + response.code() + ") - " + errorBody;
            }

            String responseBody = response.body() != null ? response.body().string() : "";

            // 4. 解析 Gemini 回傳的複雜 JSON，直接取出文字內容
            return extractTextFromResponse(responseBody);
        }
    }

    /**
     * 從 Gemini API 的回傳結果中提取純文字
     */
    private String extractTextFromResponse(String responseBody) {
        try {
            JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
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