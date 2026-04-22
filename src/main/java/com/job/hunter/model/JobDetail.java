package com.job.hunter.model;

public record JobDetail(
        String companyName,
        String jobTitle,
        String salary,
        String content,
        String condition,
        String benefit
) {}