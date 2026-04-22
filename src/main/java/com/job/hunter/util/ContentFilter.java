package com.job.hunter.util;

import com.job.hunter.model.JobDetail;
import java.util.Arrays;
import java.util.List;

public class ContentFilter {

    // 定義你想過濾掉的關鍵字 (黑名單)
    private static final List<String> BLACKLIST = Arrays.asList(
            ".*保險.*", ".*理財專員.*", ".*博弈.*", ".*客服專員.*", ".*洗碼.*"
    );

    public static boolean isGarbage(JobDetail job) {
        // 1. 檢查標題
        for (String regex : BLACKLIST) {
            if (job.jobTitle().matches(regex)) {
                System.out.println("🚫 過濾掉垃圾職缺（標題符合黑名單）：" + job.jobTitle());
                return true;
            }
        }
        //System.out.println("DEBUG: 檢查內容長度 = " + job.content().length() + " 內容是: " + job.content());

        // 2. 檢查內容是否過短 (通常是沒寫清楚的缺)
        if (job.content().length() < 5) {
            System.out.println("🚫 過濾掉內容過短的職缺：" + job.jobTitle());
            return true;
        }

        return false;
    }
}