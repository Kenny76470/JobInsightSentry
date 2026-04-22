package com.job.hunter.service;

import com.google.gson.Gson;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GeminiService {

    private final OkHttpClient client;
    private final String apiKey;
    private final Gson gson = new Gson();

    // 這裡改為 Spring 注入，或手動建立
    public GeminiService(OkHttpClient client) {
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("GEMINI_API_KEY");
        this.client = client;
    }

    public String analyze(String text) throws IOException {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey;

        // 3. 使用 Map 與 Gson 構建 JSON (結構安全，自動處理轉義)
        Map<String, Object> requestMap = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", text + " (請根據這份職缺內容，條列式分析其優缺點、建議避雷點與面試準備重點)")
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
                return "❌ AI 分析失敗 (Status: " + response.code() + ")";
            }
            // 這裡可以考慮進一步解析 JSON 只回傳 text 部分，但目前先回傳全部字串
            return response.body() != null ? response.body().string() : "無回應內容";
        }
    }
}