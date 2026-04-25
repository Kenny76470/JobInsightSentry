package com.job.hunter.service;

import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeminiServiceTest {

    private OkHttpClient mockClient;
    private Call mockCall;
    private AiService aiService; // 👈 這裡宣告為介面類型，實例化為 GeminiService

    @BeforeEach
    void setUp() {
        mockClient = mock(OkHttpClient.class);
        mockCall = mock(Call.class);
        // 初始化真實的 GeminiService 並注入 Mock 的網路組件
        aiService = new GeminiService(mockClient);
    }

    @Test
    void testAnalyze_Success() throws IOException {
        // 1. 準備模擬的 Gemini API 回傳 JSON (結構必須符合你 extractTextFromResponse 解析的邏輯)
        String fakeJsonResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "這是一份好工作，推薦應徵。" }
                        ]
                      }
                    }
                  ]
                }
                """;

        Response fakeResponse = new Response.Builder()
                .request(new Request.Builder().url("https://generativelanguage.googleapis.com/").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(fakeJsonResponse, MediaType.parse("application/json")))
                .build();

        // 2. 設定 Mock 行為
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(fakeResponse);

        // 3. 執行測試
        String result = aiService.analyze("測試用的職缺內容文字");

        // 4. 驗證結果與行為
        assertNotNull(result);
        assertTrue(result.contains("這是一份好工作"));

        // 驗證網路請求確實發生了
        verify(mockClient, times(1)).newCall(any(Request.class));
    }

    @Test
    void testAnalyze_ApiError() throws IOException {
        // 1. 模擬 API 噴出 500 錯誤
        Response errorResponse = new Response.Builder()
                .request(new Request.Builder().url("https://generativelanguage.googleapis.com/").build())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body(ResponseBody.create("Server is down", MediaType.parse("application/json")))
                .build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(errorResponse);

        // 2. 執行測試
        String result = aiService.analyze("測試內容");

        // 3. 驗證回傳的錯誤訊息 (這是你 GeminiService 裡定義的字串)
        assertTrue(result.contains("❌ AI 分析失敗"));
        assertTrue(result.contains("500"));
    }

    @Test
    void testAnalyze_JsonParseError() throws IOException {
        // 1. 模擬回傳格式錯誤的 JSON
        String brokenJson = "{\"invalid\": \"json\"}";

        Response brokenResponse = new Response.Builder()
                .request(new Request.Builder().url("https://generativelanguage.googleapis.com/").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .body(ResponseBody.create(brokenJson, MediaType.parse("application/json")))
                .build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(brokenResponse);

        // 2. 執行測試
        String result = aiService.analyze("測試內容");

        // 3. 驗證 extractTextFromResponse 捕捉到異常後的回傳訊息
        assertTrue(result.contains("⚠️ 無法解析 AI 回傳內容"));
    }
}