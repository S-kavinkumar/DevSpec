-- 1. Create system settings table
CREATE TABLE IF NOT EXISTS system_settings (
    key_name VARCHAR(255) PRIMARY KEY,
    value_content VARCHAR(1024) NOT NULL
);

-- 2. Populate system settings with defaults
INSERT INTO system_settings (key_name, value_content) VALUES ('ai_provider', 'gemini');
INSERT INTO system_settings (key_name, value_content) VALUES ('max_upload_size', '52428800');
INSERT INTO system_settings (key_name, value_content) VALUES ('workspace_location', 'c:/Users/kavin/OneDrive/Desktop/DevSpec/temp-uploads');
INSERT INTO system_settings (key_name, value_content) VALUES ('analysis_timeout', '30');
INSERT INTO system_settings (key_name, value_content) VALUES ('logging_level', 'INFO');
INSERT INTO system_settings (key_name, value_content) VALUES ('thread_pool_size', '4');
INSERT INTO system_settings (key_name, value_content) VALUES ('pdf_header', 'DEVSPEC Quality Report');
INSERT INTO system_settings (key_name, value_content) VALUES ('pdf_footer', 'Page X | Confidential');

-- 3. Add Git metadata columns to review_history
ALTER TABLE review_history ADD COLUMN repository_name VARCHAR(255) NULL;
ALTER TABLE review_history ADD COLUMN branch VARCHAR(255) NULL;
ALTER TABLE review_history ADD COLUMN commit_hash VARCHAR(100) NULL;
ALTER TABLE review_history ADD COLUMN git_tag VARCHAR(255) NULL;
ALTER TABLE review_history ADD COLUMN review_timestamp TIMESTAMP NULL;

-- 4. Add Git metadata and Technical Debt/Insights columns to reports
ALTER TABLE reports ADD COLUMN repository_name VARCHAR(255) NULL;
ALTER TABLE reports ADD COLUMN branch VARCHAR(255) NULL;
ALTER TABLE reports ADD COLUMN commit_hash VARCHAR(100) NULL;
ALTER TABLE reports ADD COLUMN git_tag VARCHAR(255) NULL;
ALTER TABLE reports ADD COLUMN review_timestamp TIMESTAMP NULL;
ALTER TABLE reports ADD COLUMN risk_assessment TEXT NULL;
ALTER TABLE reports ADD COLUMN estimated_maintainability VARCHAR(100) NULL;
ALTER TABLE reports ADD COLUMN estimated_technical_debt VARCHAR(255) NULL;
ALTER TABLE reports ADD COLUMN tech_debt_hours DOUBLE DEFAULT 0.0;
ALTER TABLE reports ADD COLUMN tech_debt_complexity VARCHAR(50) NULL;
ALTER TABLE reports ADD COLUMN tech_debt_risk VARCHAR(50) NULL;
ALTER TABLE reports ADD COLUMN tech_debt_priority VARCHAR(50) NULL;
ALTER TABLE reports ADD COLUMN review_insights_json LONGTEXT NULL;

-- 5. Add custom analysis results in analysis_results
ALTER TABLE analysis_results ADD COLUMN dependency_analysis_json LONGTEXT NULL;
ALTER TABLE analysis_results ADD COLUMN configuration_analysis_json LONGTEXT NULL;
ALTER TABLE analysis_results ADD COLUMN api_analysis_json LONGTEXT NULL;
ALTER TABLE analysis_results ADD COLUMN database_analysis_json LONGTEXT NULL;
