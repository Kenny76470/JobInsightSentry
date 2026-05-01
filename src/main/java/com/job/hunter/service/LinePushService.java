package com.job.hunter.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LinePushService {

    @Value("${line.channel.token}") // 讀取 .env 的 LINE Token
    private String token;

    // 💡 已經刪除寫死的 private String userId;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final String API_URL = "https://api.line.me/v2/bot/message/push";

    // 💡 方法新增了 String userId 參數
    public void sendPushNotification(String userId, String text) {

        // 💡 防呆：確保系統有拿到使用者的 LINE ID 才發送，避免觸發 API 錯誤
        if (userId == null || userId.isBlank()) {
            log.error("❌ LINE 推播失敗: 無法取得目標使用者的 userId");
            return;
        }

        // 構建 LINE Messaging API 要求的 JSON 格式
        JsonObject payload = new JsonObject();
        payload.addProperty("to", userId); // 將訊息發送給指定的 userId

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("type", "text");
        message.addProperty("text", text);
        messages.add(message);

        payload.add("messages", messages);

        RequestBody body = RequestBody.create(
                payload.toString(),
                MediaType.parse("application/json; charset=utf-8") // 稍微修正了 MediaType 的寫法
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("❌ LINE 推播失敗: 狀態碼 {}, 回應: {}", response.code(), response.body().string());
            } else {
                log.info("✅ LINE 推播成功");
            }
        } catch (Exception e) {
            log.error("❌ LINE 推播發生異常: ", e);
        }
    }
}