package com.job.hunter.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlValidator {

    private static final String JOB_ID_PATTERN = "job/([a-zA-Z0-9]+)";

    // 這是你原本精美的邏輯
    public static String cleanUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return null;
        String trimmedUrl = rawUrl.trim();
        Pattern pattern = Pattern.compile(JOB_ID_PATTERN);
        Matcher matcher = pattern.matcher(trimmedUrl);

        if (matcher.find()) {
            String jobId = matcher.group(1);
            return "https://www.104.com.tw/job/" + jobId;
        }
        return null;
    }

    // 💡 新增這個「別名」方法，對接 CrawlerService 的呼叫
    public static String clean(String url) {
        return cleanUrl(url);
    }
}