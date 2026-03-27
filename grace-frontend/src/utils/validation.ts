// Client-side validation rules (executed before API calls)
export const VALIDATION_RULES = {
  // Video upload
  SUPPORTED_VIDEO_FORMATS: ['mp4', 'mov', 'avi', 'mkv'],
  MAX_VIDEO_FILE_SIZE: 5 * 1024 * 1024 * 1024, // 5GB
  MIN_VIDEO_FILE_SIZE: 1024, // 1KB (minimum valid video file)

  // Metadata
  MAX_TITLE_LENGTH: 100,
  MIN_TITLE_LENGTH: 1,
  MAX_DESCRIPTION_LENGTH: 5000,
  MIN_TAGS: 5,
  MAX_TAGS: 15,
  MAX_TAG_LENGTH: 50,

  // Avatar
  SUPPORTED_AVATAR_FORMATS: ['jpg', 'jpeg', 'png'],
  MAX_AVATAR_FILE_SIZE: 2 * 1024 * 1024, // 2MB

  // Channel
  MAX_CHANNEL_NAME_LENGTH: 100,
  MAX_CHANNEL_PRIORITY: 99,
  MIN_CHANNEL_PRIORITY: 1,
}

// Client-side validation messages
export const VALIDATION_MESSAGES = {
  // Video
  UNSUPPORTED_VIDEO_FORMAT: '不支持的视频格式，请上传 MP4、MOV、AVI 或 MKV 文件',
  VIDEO_TOO_LARGE: '视频文件超过 5GB 限制',
  VIDEO_TOO_SMALL: '视频文件无效',
  VIDEO_NOT_SELECTED: '请选择视频文件',

  // Metadata
  TITLE_TOO_LONG: '标题不能超过 100 字符',
  TITLE_REQUIRED: '标题不能为空',
  DESCRIPTION_TOO_LONG: '描述不能超过 5000 字符',
  TAGS_TOO_FEW: '至少需要 5 个标签',
  TAGS_TOO_MANY: '标签不能超过 15 个',
  TAG_TOO_LONG: '单个标签不能超过 50 字符',

  // Avatar
  UNSUPPORTED_AVATAR_FORMAT: '仅支持 JPG/PNG 格式',
  AVATAR_TOO_LARGE: '头像文件不能超过 2MB',
  AVATAR_NOT_SELECTED: '请选择头像文件',

  // Channel
  CHANNEL_NAME_TOO_LONG: '渠道名称不能超过 100 字符',
  CHANNEL_NAME_REQUIRED: '渠道名称不能为空',
  CHANNEL_PRIORITY_OUT_OF_RANGE: '优先级必须在 1-99 之间',
  CHANNEL_CONFIG_REQUIRED: '渠道配置不能为空',
  CHANNEL_TYPE_REQUIRED: '请选择渠道类型',
}

// Helper functions for validation

// Validate video file
export function validateVideoFile(file: File): { valid: boolean; message?: string } {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !VALIDATION_RULES.SUPPORTED_VIDEO_FORMATS.includes(ext)) {
    return { valid: false, message: VALIDATION_MESSAGES.UNSUPPORTED_VIDEO_FORMAT }
  }
  if (file.size > VALIDATION_RULES.MAX_VIDEO_FILE_SIZE) {
    return { valid: false, message: VALIDATION_MESSAGES.VIDEO_TOO_LARGE }
  }
  if (file.size < VALIDATION_RULES.MIN_VIDEO_FILE_SIZE) {
    return { valid: false, message: VALIDATION_MESSAGES.VIDEO_TOO_SMALL }
  }
  return { valid: true }
}

// Validate video title
export function validateTitle(title: string): { valid: boolean; message?: string } {
  if (!title || title.trim().length === 0) {
    return { valid: false, message: VALIDATION_MESSAGES.TITLE_REQUIRED }
  }
  if (title.length > VALIDATION_RULES.MAX_TITLE_LENGTH) {
    return { valid: false, message: VALIDATION_MESSAGES.TITLE_TOO_LONG }
  }
  return { valid: true }
}

// Validate video description
export function validateDescription(description: string): { valid: boolean; message?: string } {
  if (description.length > VALIDATION_RULES.MAX_DESCRIPTION_LENGTH) {
    return { valid: false, message: VALIDATION_MESSAGES.DESCRIPTION_TOO_LONG }
  }
  return { valid: true }
}

// Validate tags
export function validateTags(tags: string[]): { valid: boolean; message?: string } {
  if (tags.length < VALIDATION_RULES.MIN_TAGS) {
    return { valid: false, message: VALIDATION_MESSAGES.TAGS_TOO_FEW }
  }
  if (tags.length > VALIDATION_RULES.MAX_TAGS) {
    return { valid: false, message: VALIDATION_MESSAGES.TAGS_TOO_MANY }
  }
  const longTag = tags.find(tag => tag.length > VALIDATION_RULES.MAX_TAG_LENGTH)
  if (longTag) {
    return { valid: false, message: VALIDATION_MESSAGES.TAG_TOO_LONG }
  }
  return { valid: true }
}

// Validate avatar file
export function validateAvatarFile(file: File): { valid: boolean; message?: string } {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !VALIDATION_RULES.SUPPORTED_AVATAR_FORMATS.includes(ext)) {
    return { valid: false, message: VALIDATION_MESSAGES.UNSUPPORTED_AVATAR_FORMAT }
  }
  if (file.size > VALIDATION_RULES.MAX_AVATAR_FILE_SIZE) {
    return { valid: false, message: VALIDATION_MESSAGES.AVATAR_TOO_LARGE }
  }
  return { valid: true }
}
