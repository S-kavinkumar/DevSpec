package com.devspec.service.analysis.frontend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data holder stub representing the future results of a React frontend analysis scan.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrontendAnalysisResult {
    private int componentsCount;
    private int customHooksCount;
    private List<String> detectedRoutes;
    private List<String> stateFrameworks;
    private List<String> architectureIssues;
    private double overallFrontendScore;
}
