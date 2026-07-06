package com.devspec.service.report;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ScoringEngine {
    private final double weightArchitecture;
    private final double weightCodeQuality;
    private final double weightSecurity;
    private final double weightMaintainability;
    private final double weightTesting;
    private final double weightDocumentation;
    private final double weightPerformance;

    public ScoringEngine(
            @Value("${devspec.weight.architecture:20}") double weightArchitecture,
            @Value("${devspec.weight.codeQuality:20}") double weightCodeQuality,
            @Value("${devspec.weight.security:15}") double weightSecurity,
            @Value("${devspec.weight.maintainability:15}") double weightMaintainability,
            @Value("${devspec.weight.testing:15}") double weightTesting,
            @Value("${devspec.weight.documentation:10}") double weightDocumentation,
            @Value("${devspec.weight.performance:5}") double weightPerformance) {
        this.weightArchitecture = weightArchitecture;
        this.weightCodeQuality = weightCodeQuality;
        this.weightSecurity = weightSecurity;
        this.weightMaintainability = weightMaintainability;
        this.weightTesting = weightTesting;
        this.weightDocumentation = weightDocumentation;
        this.weightPerformance = weightPerformance;
    }

    public double calculateOverallScore(
            double architectureScore,
            double codeQualityScore,
            double securityScore,
            double maintainabilityScore,
            double testingScore,
            double documentationScore,
            double performanceScore) {
        
        double totalWeight = weightArchitecture + weightCodeQuality + weightSecurity 
                + weightMaintainability + weightTesting + weightDocumentation + weightPerformance;
        
        if (totalWeight <= 0) {
            totalWeight = 100.0;
        }

        double weightedSum = (architectureScore * weightArchitecture)
                + (codeQualityScore * weightCodeQuality)
                + (securityScore * weightSecurity)
                + (maintainabilityScore * weightMaintainability)
                + (testingScore * weightTesting)
                + (documentationScore * weightDocumentation)
                + (performanceScore * weightPerformance);

        return Math.round((weightedSum / totalWeight) * 10.0) / 10.0;
    }
}
