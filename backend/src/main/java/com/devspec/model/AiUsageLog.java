package com.devspec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String provider;

    @Column(name = "tokens_used", nullable = false)
    private Integer tokensUsed;

    @Column(name = "request_time_ms", nullable = false)
    private Long requestTimeMs;

    @Column(name = "cost_estimate", nullable = false)
    private Double costEstimate;

    @Column(nullable = false)
    private String status;

    @Column(name = "username")
    private String username;

    @Column(nullable = false)
    private String operation;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
