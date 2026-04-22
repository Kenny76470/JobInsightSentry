package com.job.hunter.service;

import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeminiServiceTest {

    private OkHttpClient mockClient;
    private Call mockCall;
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        mockClient = mock(OkHttpClient.class);
        mockCall = mock(Call.class);
        // 初始化 Service，將 Mock 的 Client 丟進去
        geminiService = new GeminiService(mockClient);
    }

    @Test
    void testAnalyze_Success() throws IOException {
        // 1. 準備假的 Response
        String fakeJsonResponse = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"這是一份好工作\"}]}}]}";

        Response fakeResponse = new Response.Builder()
                .request(new Request.Builder().url("http://test.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(fakeJsonResponse, MediaType.parse("application/json")))
                .build();

        // 2. 設定 Mock 行為：當 client 執行任何請求時，回傳我們的假 Response
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(fakeResponse);

        // 3. 執行測試
        String result = geminiService.analyze("軟體工程師職缺內容");

        // 4. 驗證結果
        assertNotNull(result);
        assertTrue(result.contains("這是一份好工作"));

        // 驗證網路請求確實有被呼叫一次
        verify(mockClient, times(1)).newCall(any(Request.class));
    }

    @Test
    void testAnalyze_ApiError() throws IOException {
        // 模擬 500 錯誤
        Response errorResponse = new Response.Builder()
                .request(new Request.Builder().url("http://test.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body(ResponseBody.create("", MediaType.parse("application/json")))
                .build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(errorResponse);

        String result = geminiService.analyze("測試內容");

        // 驗證是否有回傳錯誤字串
        assertTrue(result.contains("AI 分析失敗"));
    }
}