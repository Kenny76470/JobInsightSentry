package com.job.hunter.config;

import com.microsoft.playwright.*;
import java.util.Collections;

public class BrowserConfig {
    public static BrowserContext createContext(Playwright playwright) {
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(Collections.singletonList("--disable-blink-features=AutomationControlled")));

        return browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"));
    }
}