package com.devspec.repository;

import com.devspec.model.UnitTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitTestResultRepository extends JpaRepository<UnitTestResult, Long> {
    Optional<UnitTestResult> findByProjectId(Long projectId);
}
