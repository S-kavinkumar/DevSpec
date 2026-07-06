CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    repo_url VARCHAR(512),
    file_path VARCHAR(512),
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_projects_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS analysis_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    language VARCHAR(100),
    framework VARCHAR(255),
    build_tool VARCHAR(100),
    num_packages INT DEFAULT 0,
    num_classes INT DEFAULT 0,
    num_interfaces INT DEFAULT 0,
    num_enums INT DEFAULT 0,
    num_records INT DEFAULT 0,
    num_methods INT DEFAULT 0,
    num_constructors INT DEFAULT 0,
    num_fields INT DEFAULT 0,
    static_analysis_issues_json LONGTEXT,
    scanned_files_json LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_analysis_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS unit_test_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    total_tests INT DEFAULT 0,
    passed INT DEFAULT 0,
    failed INT DEFAULT 0,
    skipped INT DEFAULT 0,
    execution_time BIGINT DEFAULT 0,
    failure_details_json LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tests_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    overall_score DOUBLE DEFAULT 0.0,
    architecture_score DOUBLE DEFAULT 0.0,
    code_quality_score DOUBLE DEFAULT 0.0,
    security_score DOUBLE DEFAULT 0.0,
    maintainability_score DOUBLE DEFAULT 0.0,
    documentation_score DOUBLE DEFAULT 0.0,
    testing_score DOUBLE DEFAULT 0.0,
    performance_score DOUBLE DEFAULT 0.0,
    report_version VARCHAR(50),
    project_version VARCHAR(50),
    reviewer VARCHAR(255),
    executive_summary TEXT,
    architecture_summary TEXT,
    tech_stack TEXT,
    security_analysis TEXT,
    strengths_json TEXT,
    weaknesses_json TEXT,
    ai_suggestions_json LONGTEXT,
    final_verdict TEXT,
    pdf_report_path VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_reports_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS review_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    analysis_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(100) NOT NULL,
    stage VARCHAR(255) NOT NULL,
    error_message TEXT,
    analysis_result_id BIGINT,
    unit_test_result_id BIGINT,
    report_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_history_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_analysis FOREIGN KEY (analysis_result_id) REFERENCES analysis_results(id) ON DELETE SET NULL,
    CONSTRAINT fk_history_tests FOREIGN KEY (unit_test_result_id) REFERENCES unit_test_results(id) ON DELETE SET NULL,
    CONSTRAINT fk_history_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255),
    operation VARCHAR(255) NOT NULL,
    status VARCHAR(100) NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Performance indices
CREATE INDEX idx_projects_user ON projects(user_id);
CREATE INDEX idx_analysis_project ON analysis_results(project_id);
CREATE INDEX idx_tests_project ON unit_test_results(project_id);
CREATE INDEX idx_reports_project ON reports(project_id);
CREATE INDEX idx_history_project ON review_history(project_id);
CREATE INDEX idx_history_analysis ON review_history(analysis_id);
