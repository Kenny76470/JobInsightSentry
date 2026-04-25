package com.job.hunter.repository;

import com.job.hunter.model.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {
    // 繼承 JpaRepository<型態, ID型態>
    // 這裡 ID 是 String，因為我們用 job_url 當主鍵
}