package com.devspec.repository;

import com.devspec.model.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {
    List<AiUsageLog> findByUsernameOrderByCreatedAtDesc(String username);
}
