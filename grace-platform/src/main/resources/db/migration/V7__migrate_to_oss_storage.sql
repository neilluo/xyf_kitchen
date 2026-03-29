-- V7: Migrate storage architecture from local filesystem to OSS
-- This migration adds OSS storage fields while maintaining backward compatibility

-- Video table: Add OSS storage fields
ALTER TABLE video 
    ADD COLUMN storage_url VARCHAR(1000) NULL AFTER file_path,
    ADD COLUMN storage_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL' AFTER storage_url;

-- UploadSession table: Add OSS storage fields
ALTER TABLE upload_session
    ADD COLUMN storage_key VARCHAR(500) NULL AFTER temp_directory,
    ADD COLUMN oss_bucket VARCHAR(100) NULL AFTER storage_key;

-- Optional: Migrate existing data from local file_path to storage_url format
-- This preserves backward compatibility: existing local files work while new uploads use OSS
-- UPDATE video SET storage_url = CONCAT('file://', file_path), storage_provider = 'LOCAL' WHERE storage_url IS NULL;

-- Note: file_path and temp_directory columns are retained for backward compatibility
-- They can be removed after confirming all existing data has been migrated to OSS
-- ALTER TABLE video DROP COLUMN file_path;
-- ALTER TABLE upload_session DROP COLUMN temp_directory;