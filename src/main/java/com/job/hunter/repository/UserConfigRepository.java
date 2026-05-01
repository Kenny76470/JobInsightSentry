package com.job.hunter.repository;

import com.job.hunter.model.UserConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConfigRepository extends JpaRepository<UserConfigEntity, Long> {
    UserConfigEntity findByUsername(String username);
}