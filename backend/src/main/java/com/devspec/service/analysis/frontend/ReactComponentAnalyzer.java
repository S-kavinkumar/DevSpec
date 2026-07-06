package com.devspec.service.analysis.frontend;

import java.io.File;
import java.util.List;

/**
 * Interface representing the React Component Analyzer framework stub.
 * Prepared for future extensions scanning JSX/TSX elements.
 */
public interface ReactComponentAnalyzer {
    
    /**
     * Inspects React functional and class components for design structure.
     * 
     * @param componentFile React component file path
     * @return List of design suggestions or component findings
     */
    List<String> analyzeComponentStructure(File componentFile);
    
    /**
     * Audits prop-types or TS interface typing configurations.
     * 
     * @param componentFile React component file path
     * @return true if props validation is active, false otherwise
     */
    boolean verifyPropsValidation(File componentFile);
}
