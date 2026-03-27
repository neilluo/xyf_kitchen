-- V6: Add performance indexes for frequently queried columns
-- Video context indexes
CREATE INDEX idx_video_status ON video(status);
CREATE INDEX idx_video_created_at ON video(created_at);
CREATE INDEX idx_upload_session_status ON upload_session(status);
CREATE INDEX idx_upload_session_expires_at ON upload_session(expires_at);

-- Metadata context indexes
CREATE INDEX idx_video_metadata_video_id ON video_metadata(video_id);

-- Distribution context indexes
CREATE INDEX idx_publish_record_video_id ON publish_record(video_id);
CREATE INDEX idx_publish_record_status ON publish_record(status);
CREATE UNIQUE INDEX uk_oauth_token_platform ON oauth_token(platform);

-- Promotion context indexes
CREATE INDEX idx_promotion_channel_status ON promotion_channel(status);
CREATE INDEX idx_promotion_channel_type ON promotion_channel(type);
CREATE INDEX idx_promotion_record_video_id ON promotion_record(video_id);
CREATE INDEX idx_promotion_record_channel_id ON promotion_record(channel_id);
CREATE INDEX idx_promotion_record_status ON promotion_record(status);
CREATE INDEX idx_promotion_record_created_at ON promotion_record(created_at);

-- User settings context indexes
CREATE INDEX idx_api_key_prefix ON api_key(prefix);
