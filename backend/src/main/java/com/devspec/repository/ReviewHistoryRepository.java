package com.devspec.repository;

import com.devspec.model.ReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewHistoryRepository extends JpaRepository<ReviewHistory, Long> {
    List<ReviewHistory> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<ReviewHistory> findByProjectUserIdOrderByCreatedAtDesc(Long userId);
    List<ReviewHistory> findByStatus(String status);
    long countByProjectUserId(Long userId);
    Optional<ReviewHistory> findByAnalysisId(String analysisId);
}
