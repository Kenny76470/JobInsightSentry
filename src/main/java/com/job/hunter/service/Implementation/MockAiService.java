package com.job.hunter.service.Implementation;

import com.job.hunter.service.AiService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev") // 👈 關鍵：開發環境 (dev) 下，Spring 會改用這一個
public class MockAiService implements AiService {

    @Override
    public String analyze(String text) {
        // 模擬延遲，讓它看起來像在思考
        try { Thread.sleep(1000); } catch (InterruptedException e) { }

        return """
                🤖 [開發模式 - 模擬分析報告]
                此職缺內容長度為：%d 字。
                
                1. 職缺亮點：這是一個適合練習 Java 的機會。
                2. 避雷點：目前為 Mock 模式，API Key 安全地躺在 .env 裡沒被使用。
                3. 面試建議：準備好你的 GitHub 作品集。
                
                (提示：若要切換回真 AI，請將 profile 改為 prod)
                """.formatted(text.length());
    }
}