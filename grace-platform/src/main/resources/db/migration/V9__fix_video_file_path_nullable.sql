-- V9: Make file_path nullable in video table
-- This allows OSS-only videos (no local file path)

ALTER TABLE video
    MODIFY COLUMN file_path VARCHAR(1000) NULL;
