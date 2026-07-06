package com.devspec.service.analysis.frontend;

import java.io.File;
import java.util.List;

/**
 * Interface representing the React Router Analyzer stub.
 * Prepared for future extensions auditing React Router configurations.
 */
public interface ReactRoutingAnalyzer {

    /**
     * Identifies configured router endpoints.
     *
     * @param routerFile React Router configuration file
     * @return List of route paths
     */
    List<String> extractConfiguredRoutes(File routerFile);

    /**
     * Scans for route protection (guarded routes, authentication triggers).
     *
     * @param routerFile React Router configuration file
     * @return List of safety findings on routes
     */
    List<String> auditGuardedRoutes(File routerFile);
}
