import { useState, useCallback, useRef } from 'react'
import { Icon } from '@/components/ui/Icon'
import { Button } from '@/components/ui/Button'

// Supported video formats
const SUPPORTED_FORMATS = ['mp4', 'mov', 'avi', 'mkv']
const MAX_FILE_SIZE = 5 * 1024 * 1024 * 1024 // 5GB in bytes

interface ValidationError {
  message: string
  code: 'FORMAT' | 'SIZE'
}

function validateFile(file: File): ValidationError | null {
  const ext = file.name.split('.').pop()?.toLowerCase()

  if (!ext || !SUPPORTED_FORMATS.includes(ext)) {
    return {
      message: `不支持的视频格式，请上传 ${SUPPORTED_FORMATS.join('、').toUpperCase()} 文件`,
      code: 'FORMAT',
    }
  }

  if (file.size > MAX_FILE_SIZE) {
    return {
      message: '文件大小超过 5GB 限制',
      code: 'SIZE',
    }
  }

  return null
}

interface DropZoneProps {
  onFileSelect: (file: File) => void
  onValidationError: (error: ValidationError) => void
}

function DropZone({ onFileSelect, onValidationError }: DropZoneProps) {
  const [isDragging, setIsDragging] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleDragEnter = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(true)
  }, [])

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }, [])

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
  }, [])

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      e.stopPropagation()
      setIsDragging(false)

      const files = e.dataTransfer.files
      if (files.length > 0) {
        const file = files[0]
        const error = validateFile(file)
        if (error) {
          onValidationError(error)
        } else {
          onFileSelect(file)
        }
      }
    },
    [onFileSelect, onValidationError]
  )

  const handleClick = useCallback(() => {
    fileInputRef.current?.click()
  }, [])

  const handleFileInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const files = e.target.files
      if (files && files.length > 0) {
        const file = files[0]
        const error = validateFile(file)
        if (error) {
          onValidationError(error)
        } else {
          onFileSelect(file)
        }
      }
      // Reset input so the same file can be selected again
      e.target.value = ''
    },
    [onFileSelect, onValidationError]
  )

  return (
    <div
      className="relative rounded-xl p-12 text-center cursor-pointer overflow-hidden"
      style={{
        backgroundColor: isDragging ? 'rgba(0, 87, 194, 0.1)' : 'rgba(240, 245, 255, 0.5)',
      }}
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      onClick={handleClick}
    >
      {/* SVG Dashed Border */}
      <svg
        className="absolute inset-0 w-full h-full pointer-events-none"
        style={{ borderRadius: '0.75rem' }}
      >
        <rect
          rx="12"
          ry="12"
          stroke="#0057c2"
          strokeOpacity={isDragging ? 0.6 : 0.3}
          strokeWidth="2"
          strokeDasharray="8 4"
          fill="none"
          width="100%"
          height="100%"
        />
      </svg>

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center">
        {/* Upload Icon */}
        <div className="w-20 h-20 bg-primary-fixed rounded-full flex items-center justify-center mb-6">
          <Icon
            name="cloud_upload"
            size={48}
            className="text-primary"
            style={{ fontVariationSettings: "'FILL' 1" }}
          />
        </div>

        {/* Main Text */}
        <h2 className="font-headline text-lg font-bold text-on-surface">
          拖拽视频文件到此处
        </h2>

        {/* Sub Text */}
        <p className="font-body text-sm text-on-surface-variant mt-2">
          或点击选择文件
        </p>

        {/* Format Hint */}
        <p className="font-body text-xs text-on-surface-variant/60 mt-4">
          支持 MP4、MOV、AVI、MKV，最大 5GB
        </p>
      </div>

      {/* Hidden File Input */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".mp4,.mov,.avi,.mkv,video/mp4,video/quicktime,video/x-msvideo,video/x-matroska"
        className="hidden"
        onChange={handleFileInputChange}
      />
    </div>
  )
}

interface ValidationErrorToastProps {
  error: ValidationError | null
  onClose: () => void
}

function ValidationErrorToast({ error, onClose }: ValidationErrorToastProps) {
  if (!error) return null

  return (
    <div className="fixed bottom-6 right-6 z-50 animate-slide-up">
      <div className="bg-error-container text-on-error-container px-6 py-4 rounded-lg shadow-lg flex items-center gap-3">
        <Icon name="error" size={20} className="text-error" />
        <span className="font-body text-sm">{error.message}</span>
        <button
          onClick={onClose}
          className="ml-2 p-1 hover:bg-error/10 rounded transition-colors"
        >
          <Icon name="close" size={16} />
        </button>
      </div>
    </div>
  )
}

export function VideoUploadPage() {
  const [validationError, setValidationError] = useState<ValidationError | null>(null)

  const handleFileSelect = useCallback((file: File) => {
    // TODO: Start upload process
    console.log('File selected:', file.name, 'Size:', file.size)
  }, [])

  const handleValidationError = useCallback((error: ValidationError) => {
    setValidationError(error)
    // Auto-dismiss after 5 seconds
    setTimeout(() => setValidationError(null), 5000)
  }, [])

  const handleCloseError = useCallback(() => {
    setValidationError(null)
  }, [])

  return (
    <div className="p-8 max-w-5xl mx-auto">
      {/* Page Header */}
      <div className="mb-12">
        <h1 className="font-headline text-[2.75rem] font-bold text-on-surface tracking-tight">
          上传视频
        </h1>
        <p className="text-slate-500 mt-2 font-body">
          将您的烹饪灵感分享给世界，支持多平台一键分发。
        </p>
      </div>

      {/* Upload Area Section */}
      <section className="bg-surface-container-lowest rounded-xl p-4 mb-8">
        <DropZone
          onFileSelect={handleFileSelect}
          onValidationError={handleValidationError}
        />
      </section>

      {/* Validation Error Toast */}
      <ValidationErrorToast error={validationError} onClose={handleCloseError} />
    </div>
  )
}
