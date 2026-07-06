package com.devspec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "unit_test_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitTestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "total_tests")
    private int totalTests;

    private int passed;
    private int failed;
    private int skipped;

    @Column(name = "execution_time")
    private long executionTime; // In milliseconds

    @Column(name = "failure_details_json", columnDefinition = "LONGTEXT")
    private String failureDetailsJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
