package com.job.hunter.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlValidator {

    // 104 職缺頁面的標準正則表達式 (只抓取 job/ 後面的 ID)
    private static final String JOB_ID_PATTERN = "job/([a-zA-Z0-9]+)";

    public static String cleanUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return null;

        // A. 去除前後空白
        String trimmedUrl = rawUrl.trim();

        // B. 處理短網址或追蹤參數 (只提取核心 Job ID 並重組)
        // 範例：https://www.104.com.tw/job/86xxx?utm_source=... -> https://www.104.com.tw/job/86xxx
        Pattern pattern = Pattern.compile(JOB_ID_PATTERN);
        Matcher matcher = pattern.matcher(trimmedUrl);

        if (matcher.find()) {
            String jobId = matcher.group(1);
            return "https://www.104.com.tw/job/" + jobId;
        }

        // C. 如果完全不符合 104 格式，回傳 null 攔截
        return null;
    }
}