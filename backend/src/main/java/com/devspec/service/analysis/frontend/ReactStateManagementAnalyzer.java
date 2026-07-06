package com.devspec.service.analysis.frontend;

import java.io.File;
import java.util.List;

/**
 * Interface representing the React State Management Analyzer stub.
 * Prepared for future extensions scanning Redux slices, Context APIs, Zustand stores, or Jotai atoms.
 */
public interface ReactStateManagementAnalyzer {

    /**
     * Identifies state management frameworks utilized.
     *
     * @param projectDir Frontend codebase directory
     * @return List of state framework labels detected
     */
    List<String> identifyStateFrameworks(File projectDir);

    /**
     * Evaluates state mutation patterns (e.g., direct state reassignments, stale state hooks).
     *
     * @param stateFile React file referencing state
     * @return List of state mutation errors
     */
    List<String> auditStateMutations(File stateFile);
}
