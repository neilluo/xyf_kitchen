-- V8: Add upload_mode field to upload_session table
-- This field distinguishes between DIRECT_OSS and SERVER_UPLOAD modes

ALTER TABLE upload_session
    ADD COLUMN upload_mode VARCHAR(20) NOT NULL DEFAULT 'DIRECT_OSS' AFTER status;

-- Update existing records to set appropriate upload_mode
-- Records with temp_directory set are likely SERVER_UPLOAD mode
UPDATE upload_session 
SET upload_mode = 'SERVER_UPLOAD' 
WHERE temp_directory IS NOT NULL AND temp_directory != '';
