package com.job.hunter.service.Implementation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.job.hunter.service.AiService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Primary
@Service
public class OllamaService implements AiService {

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    // 💡 修正 1：改用 /api/chat 端點，這對現代對話模型絕對穩定
    private final String OLLAMA_API_URL = "http://localhost:11434/api/chat";

    // ⚠️ 請確保這裡填寫的名字，跟你終端機裡 ollama run 的名字「一模一樣」
    // 如果你抓的是 2.5，請改成 "qwen2.5"
    private final String MODEL_NAME = "qwen3.5:9b";

    public OllamaService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String analyze(String prompt) throws IOException {
        // 💡 修正 2：改用 messages 陣列格式來模擬對話
        Map<String, Object> requestMap = Map.of(
                "model", MODEL_NAME,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "stream", false,
                "format", "json"
        );

        String jsonPayload = gson.toJson(requestMap);

        RequestBody body = RequestBody.create(
                jsonPayload,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(OLLAMA_API_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown Error";
                log.error("❌ Ollama API 失敗: {} - {}", response.code(), errorBody);
                throw new IOException("HTTP " + response.code() + " 錯誤: " + errorBody);
            }

            String responseBody = response.body().string();
            return extractTextFromResponse(responseBody);
        }
    }

    private String extractTextFromResponse(String responseBody) throws IOException {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            // 💡 修正 3：/api/chat 的回傳格式不同，文字藏在 message -> content 裡面
            return json.getAsJsonObject("message").get("content").getAsString();
        } catch (Exception e) {
            log.error("⚠️ Ollama 解析 JSON 失敗: {}", e.getMessage());
            throw new IOException("Ollama JSON 結構解析失敗");
        }
    }
}