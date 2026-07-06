package com.devspec.service.analysis.frontend;

import java.io.File;
import java.util.List;

/**
 * Interface representing the React Custom Hooks Analyzer stub.
 * Prepared for future extensions scanning React custom hooks, rules of hooks, and closures.
 */
public interface ReactHookAnalyzer {

    /**
     * Scans for dependency array compliance in hooks (useEffect, useMemo, useCallback).
     *
     * @param hookFile React code file path
     * @return List of dependency warning logs
     */
    List<String> auditHookDependencies(File hookFile);

    /**
     * Evaluates custom hooks design guidelines.
     *
     * @param hookFile React code file path
     * @return List of suggestions
     */
    List<String> analyzeCustomHooks(File hookFile);
}
