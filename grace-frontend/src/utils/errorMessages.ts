// Error code to user message mapping (1001-9999)
export const ERROR_MESSAGES: Record<number, string> = {
  // Video errors (1001-1099)
  1001: '不支持的视频格式，请上传 MP4、MOV、AVI 或 MKV 文件',
  1002: '视频文件超过 5GB 限制',
  1003: '上传会话不存在或已过期，请重新上传',
  1004: '上传会话已超时，请重新上传',
  1005: '分片索引超出范围',
  1006: '重复上传分片',
  1007: '尚有分片未上传完成',
  1008: '视频不存在',

  // Metadata errors (2001-2099)
  2001: '元数据校验失败，请检查标题和描述',
  2002: '元数据不存在',
  2003: '元数据已确认，无法再编辑',
  2004: '视频尚未上传完成',

  // Distribution errors (3001-3099)
  3001: '不支持的分发平台',
  3002: '平台授权已过期，请重新连接',
  3003: '平台未授权，请先完成连接',
  3004: '平台配额超限，请稍后重试',
  3005: '视频尚未就绪，请先确认元数据',
  3006: '发布任务不存在',
  3007: '平台 API 异常，请稍后重试',
  3008: '视频未就绪，请先确认元数据',
  3009: 'OAuth 授权失败',

  // Promotion errors (4001-4099)
  4001: '推广渠道不存在',
  4002: '渠道配置无效',
  4003: '推广渠道已被禁用',
  4004: '推广记录不存在',
  4005: '无效的推广状态',

  // User/Settings errors (5001-5099)
  5001: '用户资料不存在',
  5002: 'API Key 不存在',
  5003: '文件上传失败',
  5004: '不支持的文件格式',
  5005: '文件超过大小限制',
  5006: '用户已存在',

  // Infrastructure errors (9001-9099)
  9001: 'AI 服务暂时不可用，请稍后重试',
  9002: '推广执行失败，请稍后重试',
  9003: '加密服务异常',

  // General errors (9999)
  9999: '系统内部错误，请稍后重试',
}

// Get user-friendly error message by error code
export function getErrorMessage(code: number, defaultMessage?: string): string {
  return ERROR_MESSAGES[code] ?? defaultMessage ?? '发生未知错误'
}
