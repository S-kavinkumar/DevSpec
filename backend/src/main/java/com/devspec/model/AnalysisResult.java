package com.devspec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private String language;
    private String framework;

    @Column(name = "build_tool")
    private String buildTool;

    @Column(name = "num_packages")
    private int numPackages;

    @Column(name = "num_classes")
    private int numClasses;

    @Column(name = "num_interfaces")
    private int numInterfaces;

    @Column(name = "num_enums")
    private int numEnums;

    @Column(name = "num_records")
    private int numRecords;

    @Column(name = "num_methods")
    private int numMethods;

    @Column(name = "num_constructors")
    private int numConstructors;

    @Column(name = "num_fields")
    private int numFields;

    @Column(name = "static_analysis_issues_json", columnDefinition = "LONGTEXT")
    private String staticAnalysisIssuesJson;

    @Column(name = "scanned_files_json", columnDefinition = "LONGTEXT")
    private String scannedFilesJson;

    @Column(name = "dependency_analysis_json", columnDefinition = "LONGTEXT")
    private String dependencyAnalysisJson;

    @Column(name = "configuration_analysis_json", columnDefinition = "LONGTEXT")
    private String configurationAnalysisJson;

    @Column(name = "api_analysis_json", columnDefinition = "LONGTEXT")
    private String apiAnalysisJson;

    @Column(name = "database_analysis_json", columnDefinition = "LONGTEXT")
    private String databaseAnalysisJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
