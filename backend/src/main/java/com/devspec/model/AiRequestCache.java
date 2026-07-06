package com.devspec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_request_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRequestCache {
    @Id
    @Column(name = "prompt_hash", length = 64)
    private String promptHash;

    @Column(name = "response_text", nullable = false, columnDefinition = "LONGTEXT")
    private String responseText;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
