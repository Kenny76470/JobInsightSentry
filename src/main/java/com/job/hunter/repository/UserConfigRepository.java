package com.job.hunter.repository;

import com.job.hunter.model.UserConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConfigRepository extends JpaRepository<UserConfigEntity, Long> {
    // 除了預設功能，我們多加一個：根據名字找人
    UserConfigEntity findByUsername(String username);
}