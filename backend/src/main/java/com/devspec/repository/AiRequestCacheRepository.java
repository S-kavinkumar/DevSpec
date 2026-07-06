package com.devspec.repository;

import com.devspec.model.AiRequestCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiRequestCacheRepository extends JpaRepository<AiRequestCache, String> {
}
