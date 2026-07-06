package com.devspec.service.analysis.frontend;

import java.io.File;
import java.util.List;

/**
 * Interface representing the React Codebase structure Analyzer stub.
 * Prepared for future extensions validating package architectures and folder organizations.
 */
public interface ReactCodeStructureAnalyzer {

    /**
     * Inspects project folder structures for standard React layouts.
     *
     * @param frontendRoot Directory of the React project
     * @return List of organization improvement suggestions
     */
    List<String> auditFolderOrganization(File frontendRoot);

    /**
     * Identifies dead components (components declared but never imported or rendered).
     *
     * @param frontendRoot Directory of the React project
     * @return List of unused components
     */
    List<String> auditDeadComponents(File frontendRoot);
}
