-- V10: Add ext_info JSON column to video table
-- 用于存储扩展信息（traceId / rpcId / createdTraceId 等），便于从 DB 追踪完整链路
--
-- 幂等说明：MySQL 8.0 的 ALTER TABLE 不支持 ADD COLUMN IF NOT EXISTS，
-- 这里用存储过程 + information_schema 做存在性判断，重复执行不会报 1060。

DROP PROCEDURE IF EXISTS grace_add_video_ext_info;

CREATE PROCEDURE grace_add_video_ext_info()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'video'
          AND COLUMN_NAME = 'ext_info'
    ) THEN
        ALTER TABLE video
            ADD COLUMN ext_info TEXT NULL COMMENT 'JSON 扩展字段，存放 traceId / rpcId / createdTraceId 等';
    END IF;
END;

CALL grace_add_video_ext_info();

DROP PROCEDURE IF EXISTS grace_add_video_ext_info;
