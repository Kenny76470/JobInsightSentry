package com.job.hunter.service.Implementation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.job.hunter.model.JobDetail;
import com.job.hunter.service.AiService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
//@Primary
@Service
public class GeminiService implements AiService {

    private final OkHttpClient client;
    private final String apiKey;
    private final Gson gson = new Gson();

    public GeminiService() {
        Dotenv dotenv = Dotenv.load();
        // 確保 API Key 讀取時不會帶有前後空白
        String rawKey = dotenv.get("GEMINI_API_KEY");
        this.apiKey = (rawKey != null) ? rawKey.trim() : "";

        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String analyze(String text) throws IOException {
        return callGemini(text);
    }

    /**
     * 底層調用 Gemini API
     */
    private String callGemini(String prompt) throws IOException {
        // 移除 URL 字串中可能的 Markdown 連結標籤與空白
        String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent";

        HttpUrl parsedUrl = HttpUrl.parse(baseUrl.trim());
        if (parsedUrl == null) {
            log.error("❌ 無法解析 URL，請檢查 baseUrl 格式：{}", baseUrl);
            throw new IOException("API URL 解析失敗");
        }

        HttpUrl url = parsedUrl.newBuilder()
                .addQueryParameter("key", apiKey)
                .build();

        // 加入 generationConfig，強制 AI 輸出純 JSON 結構
        Map<String, Object> requestMap = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                },
                "generationConfig", Map.of(
                        "response_mime_type", "application/json"
                )
        );

        String jsonPayload = gson.toJson(requestMap);

        RequestBody body = RequestBody.create(
                jsonPayload,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown Error";
                log.error("❌ Gemini API 失敗: {} - {}", response.code(), errorBody);

                // 💡 關鍵修正：不再回傳假 JSON，直接拋出 Exception！
                // 這樣外層的 JobAnalysisService 才能 catch 到 429 錯誤字眼並正確觸發 60 秒冷卻
                throw new IOException("HTTP " + response.code() + " 錯誤: " + errorBody);
            }

            String responseBody = response.body().string();
            return extractTextFromResponse(responseBody);
        }
    }

    private String extractTextFromResponse(String responseBody) throws IOException {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            return json.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            log.error("⚠️ AI 解析 JSON 失敗: {}", e.getMessage());
            // 如果連 JSON 結構都爛了，一樣拋出錯誤讓系統知道失敗了，而不是塞 0 分
            throw new IOException("JSON 結構解析失敗");
        }
    }
}