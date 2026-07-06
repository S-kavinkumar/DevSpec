package com.devspec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@SQLDelete(sql = "UPDATE reports SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "overall_score")
    private double overallScore;

    @Column(name = "architecture_score")
    private double architectureScore;

    @Column(name = "code_quality_score")
    private double codeQualityScore;

    @Column(name = "security_score")
    private double securityScore;

    @Column(name = "maintainability_score")
    private double maintainabilityScore;

    @Column(name = "documentation_score")
    private double documentationScore;

    @Column(name = "testing_score")
    private double testingScore;

    @Column(name = "performance_score")
    private double performanceScore;

    @Column(name = "report_version")
    private String reportVersion;

    @Column(name = "project_version")
    private String projectVersion;

    @Column(name = "reviewer")
    private String reviewer;

    @Column(name = "executive_summary", columnDefinition = "TEXT")
    private String executiveSummary;

    @Column(name = "architecture_summary", columnDefinition = "TEXT")
    private String architectureSummary;

    @Column(name = "tech_stack", columnDefinition = "TEXT")
    private String techStack;

    @Column(name = "security_analysis", columnDefinition = "TEXT")
    private String securityAnalysis;

    @Column(name = "strengths_json", columnDefinition = "TEXT")
    private String strengthsJson; // List of strengths (JSON)

    @Column(name = "weaknesses_json", columnDefinition = "TEXT")
    private String weaknessesJson; // List of weaknesses (JSON)

    @Column(name = "ai_suggestions_json", columnDefinition = "LONGTEXT")
    private String aiSuggestionsJson; // List of AI suggestions (JSON)

    @Column(name = "final_verdict", columnDefinition = "TEXT")
    private String finalVerdict;

    @Column(name = "pdf_report_path")
    private String pdfReportPath;

    @Column(name = "repository_name")
    private String repositoryName;

    private String branch;

    @Column(name = "commit_hash")
    private String commitHash;

    @Column(name = "git_tag")
    private String gitTag;

    @Column(name = "review_timestamp")
    private LocalDateTime reviewTimestamp;

    @Column(name = "risk_assessment", columnDefinition = "TEXT")
    private String riskAssessment;

    @Column(name = "estimated_maintainability")
    private String estimatedMaintainability;

    @Column(name = "estimated_technical_debt")
    private String estimatedTechnicalDebt;

    @Column(name = "tech_debt_hours")
    private double techDebtHours;

    @Column(name = "tech_debt_complexity")
    private String techDebtComplexity;

    @Column(name = "tech_debt_risk")
    private String techDebtRisk;

    @Column(name = "tech_debt_priority")
    private String techDebtPriority;

    @Column(name = "review_insights_json", columnDefinition = "LONGTEXT")
    private String reviewInsightsJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Version
    private Long version;
}
