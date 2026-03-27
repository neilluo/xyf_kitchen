package com.grace.platform.shared;

public final class ErrorCode {
    private ErrorCode() {}

    // 成功
    public static final int SUCCESS = 0;

    // ===== 1001-1099: Video Context =====
    public static final int UNSUPPORTED_VIDEO_FORMAT      = 1001;
    public static final int VIDEO_FILE_SIZE_EXCEEDED       = 1002;
    public static final int UPLOAD_SESSION_NOT_FOUND       = 1003;
    public static final int UPLOAD_SESSION_EXPIRED         = 1004;
    public static final int CHUNK_INDEX_OUT_OF_RANGE       = 1005;
    public static final int DUPLICATE_CHUNK                = 1006;
    public static final int UPLOAD_NOT_COMPLETE            = 1007;
    public static final int VIDEO_NOT_FOUND                = 1008;

    // ===== 2001-2099: Metadata Context =====
    public static final int INVALID_METADATA               = 2001;
    public static final int METADATA_NOT_FOUND             = 2002;
    public static final int METADATA_ALREADY_CONFIRMED     = 2003;
    public static final int VIDEO_NOT_UPLOADED             = 2004;

    // ===== 3001-3099: Distribution Context =====
    public static final int UNSUPPORTED_PLATFORM           = 3001;
    public static final int PLATFORM_AUTH_EXPIRED          = 3002;
    public static final int PLATFORM_NOT_AUTHORIZED        = 3003;
    public static final int PLATFORM_QUOTA_EXCEEDED        = 3004;
    public static final int VIDEO_NOT_READY                = 3005;
    public static final int PUBLISH_TASK_NOT_FOUND         = 3006;
    public static final int PLATFORM_API_ERROR             = 3007;

    // ===== 4001-4099: Promotion Context =====
    public static final int CHANNEL_NOT_FOUND              = 4001;
    public static final int INVALID_CHANNEL_CONFIG         = 4002;
    public static final int CHANNEL_DISABLED               = 4003;
    public static final int PROMOTION_RECORD_NOT_FOUND     = 4004;

    // ===== 5001-5099: User/Settings Context =====
    public static final int PROFILE_NOT_FOUND              = 5001;
    public static final int API_KEY_NOT_FOUND              = 5002;

    // ===== 9001-9099: Infrastructure =====
    public static final int LLM_SERVICE_UNAVAILABLE        = 9001;
    public static final int OPENCRAWL_EXECUTION_FAILED     = 9002;
    public static final int ENCRYPTION_ERROR               = 9003;
    public static final int INTERNAL_SERVER_ERROR          = 9999;
}
