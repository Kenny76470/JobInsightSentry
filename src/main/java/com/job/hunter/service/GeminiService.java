package com.job.hunter.service;

import com.job.hunter.model.JobDetail;
import okhttp3.*;
import com.google.gson.*;
import java.io.IOException;


public class GeminiService {
    // 1. 把你那個 AIza 開頭的 Key 貼在這裡
    private static final String API_KEY = io.github.cdimascio.dotenv.Dotenv.load().get("GEMINI_KEY");;
    // 使用 Gemini 3.1 Flash Lite Preview
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=" + API_KEY;
    public static String analyze(JobDetail job) {
        OkHttpClient client = new OkHttpClient();

        // 構造 Prompt
        String prompt = String.format("""
            你是一位資深技術獵頭。請分析以下職缺資訊：
            公司：%s
            職稱：%s
            內容：%s
            條件：%s
            
            請給我三點分析：
            1. 這個職缺的核心技術門檻。
            2. 建議的投遞策略。
            3. 這個職缺的市場競爭力（1-10分）。
            """, job.companyName(), job.jobTitle(), job.content(), job.condition());

        // 構造 JSON
        JsonObject json = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject part = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        parts.add(textPart);
        part.add("parts", parts);
        contents.add(part);
        json.add("contents", contents);

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(API_URL).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject resJson = JsonParser.parseString(responseBody).getAsJsonObject();
                return resJson.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
            } else {
                // 這裡很重要：如果沒成功，看它是噴什麼錯誤碼
                String errorMsg = response.body() != null ? response.body().string() : "Unknown Error";
                return "❌ AI 分析失敗，狀態碼：" + response.code() + "\n錯誤詳情：" + errorMsg;
            }
        } catch (IOException e) {
            return "❌ 網路連線失敗：" + e.getMessage();
        }
    }
}