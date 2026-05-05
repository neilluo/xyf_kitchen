-- V10: Add ext_info JSON column to video table
-- 用于存储扩展信息（traceId / rpcId / createdTraceId 等），便于从 DB 追踪完整链路

ALTER TABLE video
    ADD COLUMN ext_info TEXT NULL COMMENT 'JSON 扩展字段，存放 traceId / rpcId / createdTraceId 等';
