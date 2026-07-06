package com.devspec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String status; // UPLOADING, VALIDATING, EXTRACTING, ANALYZING_PROJECT, RUNNING_STATIC_ANALYSIS, EXECUTING_UNIT_TESTS, GENERATING_AI_REVIEW, CREATING_FINAL_REPORT, COMPLETED, FAILED

    @Column(nullable = false)
    private String stage; // Display status name

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_result_id")
    private AnalysisResult analysisResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_test_result_id")
    private UnitTestResult unitTestResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private Report report;

    @Column(name = "analysis_id", unique = true, nullable = false)
    private String analysisId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "repository_name")
    private String repositoryName;

    private String branch;

    @Column(name = "commit_hash")
    private String commitHash;

    @Column(name = "git_tag")
    private String gitTag;

    @Column(name = "review_timestamp")
    private LocalDateTime reviewTimestamp;

    @Version
    private Long version;
}
