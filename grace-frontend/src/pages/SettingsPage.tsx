import { useState, useRef, useCallback } from 'react'
import {
  useProfile,
  useUpdateProfile,
  useUploadAvatar,
  useConnectedAccounts,
  useNotifications,
  useUpdateNotifications,
  useApiKeys,
  useCreateApiKey,
  useDeleteApiKey,
} from '@/hooks/useSettings'
import { initiatePlatformAuth } from '@/api/distribution'
import { Icon } from '@/components/ui/Icon'
import { Button } from '@/components/ui/Button'
import { Toggle } from '@/components/ui/Toggle'
import { Input } from '@/components/ui/Input'
import type { ConnectedAccount, ApiKey, ApiKeyWithSecret } from '@/types/settings'

const platformConfig: Record<string, {
  displayName: string
  icon: string
  bgColor: string
  iconColor: string
}> = {
  youtube: {
    displayName: 'YouTube',
    icon: 'video_call',
    bgColor: 'bg-red-50',
    iconColor: 'text-red-600',
  },
  weibo: {
    displayName: 'Weibo',
    icon: 'public',
    bgColor: 'bg-orange-50',
    iconColor: 'text-orange-600',
  },
  bilibili: {
    displayName: 'Bilibili',
    icon: 'tv',
    bgColor: 'bg-sky-50',
    iconColor: 'text-sky-500',
  },
}

function SectionTitle({ title, icon }: { title: string; icon: string }) {
  return (
    <h3 className="text-xl font-bold font-headline flex items-center gap-2 mb-8">
      <Icon name={icon} size={20} className="text-primary" />
      {title}
    </h3>
  )
}

function ProfileSection() {
  const { data: profile, isLoading } = useProfile()
  const updateProfile = useUpdateProfile()
  const uploadAvatar = useUploadAvatar()
  const [isEditing, setIsEditing] = useState(false)
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleEditClick = useCallback(() => {
    if (profile) {
      setDisplayName(profile.displayName)
      setEmail(profile.email ?? '')
      setIsEditing(true)
    }
  }, [profile])

  const handleSave = useCallback(() => {
    updateProfile.mutate(
      { displayName, email },
      {
        onSuccess: () => {
          setIsEditing(false)
        },
      }
    )
  }, [displayName, email, updateProfile])

  const handleAvatarChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (!['image/jpeg', 'image/png', 'image/jpg'].includes(file.type)) {
      alert('仅支持 JPG/PNG 格式')
      return
    }

    if (file.size > 2 * 1024 * 1024) {
      alert('头像文件不能超过 2MB')
      return
    }

    uploadAvatar.mutate(file)
  }, [uploadAvatar])

  if (isLoading) {
    return (
      <section className="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
        <SectionTitle title="用户资料" icon="person" />
        <div className="flex items-center gap-8">
          <div className="w-32 h-32 rounded-full bg-surface-container-high animate-pulse" />
          <div className="flex-1 space-y-4">
            <div className="h-8 w-48 bg-surface-container-high rounded animate-pulse" />
            <div className="h-4 w-64 bg-surface-container-high rounded animate-pulse" />
          </div>
        </div>
      </section>
    )
  }

  return (
    <section className="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
      <SectionTitle title="用户资料" icon="person" />
      <div className="flex items-center gap-8">
        <div className="relative group">
          <img
            alt={profile?.displayName ?? 'User Avatar'}
            className="w-32 h-32 rounded-full object-cover ring-4 ring-surface-container-low"
            src={profile?.avatarUrl ?? 'https://via.placeholder.com/128?text=Avatar'}
          />
          <button
            className="absolute bottom-0 right-0 bg-primary text-white p-2 rounded-full shadow-lg hover:scale-110 transition-transform"
            onClick={() => fileInputRef.current?.click()}
          >
            <Icon name="photo_camera" size={16} />
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/jpg"
            className="hidden"
            onChange={handleAvatarChange}
          />
        </div>
        <div className="flex-1">
          {isEditing ? (
            <div className="space-y-4">
              <div>
                <label className="text-xs text-on-surface-variant font-medium mb-1 block">
                  显示名称
                </label>
                <Input
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="输入显示名称"
                />
              </div>
              <div>
                <label className="text-xs text-on-surface-variant font-medium mb-1 block">
                  邮箱地址
                </label>
                <Input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="输入邮箱地址"
                />
              </div>
              <div className="flex gap-3 mt-4">
                <Button variant="primary" onClick={handleSave} disabled={updateProfile.isPending}>
                  {updateProfile.isPending ? '保存中...' : '保存'}
                </Button>
                <Button variant="secondary" onClick={() => setIsEditing(false)}>
                  取消
                </Button>
              </div>
            </div>
          ) : (
            <>
              <h4 className="text-2xl font-extrabold text-on-surface font-headline">
                {profile?.displayName ?? '用户'}
              </h4>
              <p className="text-secondary font-medium mb-4 font-body">
                {profile?.email ?? '未设置邮箱'}
              </p>
              <div className="flex gap-3">
                <Button
                  variant="primary"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploadAvatar.isPending}
                >
                  {uploadAvatar.isPending ? '上传中...' : '更换头像'}
                </Button>
                <Button variant="secondary" onClick={handleEditClick}>
                  编辑资料
                </Button>
              </div>
            </>
          )}
        </div>
      </div>
    </section>
  )
}

interface AccountRowProps {
  account: ConnectedAccount
  onConnect: (platform: string) => void
}

function AccountRow({ account, onConnect }: AccountRowProps) {
  const config = platformConfig[account.platform.toLowerCase()] ?? {
    displayName: account.displayName,
    icon: 'link',
    bgColor: 'bg-surface-container-high',
    iconColor: 'text-on-surface-variant',
  }

  return (
    <div className="flex items-center justify-between p-4 bg-surface rounded-lg">
      <div className="flex items-center gap-4">
        <div className={`w-10 h-10 rounded-full ${config.bgColor} flex items-center justify-center`}>
          <Icon name={config.icon} size={20} className={config.iconColor} />
        </div>
        <div>
          <p className="font-bold font-body text-sm">{config.displayName}</p>
          <p className="text-xs text-secondary font-body">
            {account.authorized && account.accountName ? account.accountName : '未连接'}
          </p>
        </div>
      </div>
      {account.authorized ? (
        <div className="flex items-center gap-2 text-emerald-600 font-semibold text-sm">
          <Icon name="check_circle" size={16} className="text-emerald-600" style={{ fontVariationSettings: "'FILL' 1" }} />
          已连接
        </div>
      ) : (
        <Button
          variant="primary"
          className="bg-primary-fixed text-on-primary-fixed hover:bg-primary-fixed-dim"
          onClick={() => onConnect(account.platform)}
        >
          连接
        </Button>
      )}
    </div>
  )
}

function ConnectedAccountsSection() {
  const { data: accounts, isLoading } = useConnectedAccounts()

  const handleConnect = useCallback(async (platform: string) => {
    try {
      const redirectUri = `${window.location.origin}/settings/callback`
      const result = await initiatePlatformAuth(platform, { redirectUri })
      window.location.href = result.authUrl
    } catch {
      alert('平台授权失败，请重试')
    }
  }, [])

  if (isLoading) {
    return (
      <section className="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
        <SectionTitle title="已连接账户" icon="link" />
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-16 bg-surface rounded-lg animate-pulse" />
          ))}
        </div>
      </section>
    )
  }

  const defaultAccounts: ConnectedAccount[] = [
    { platform: 'youtube', displayName: 'YouTube', authorized: false, accountName: null, connectedAt: null },
    { platform: 'weibo', displayName: 'Weibo', authorized: false, accountName: null, connectedAt: null },
    { platform: 'bilibili', displayName: 'Bilibili', authorized: false, accountName: null, connectedAt: null },
  ]

  const displayAccounts = accounts ?? defaultAccounts

  return (
    <section className="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
      <SectionTitle title="已连接账户" icon="link" />
      <div className="space-y-4">
        {displayAccounts.map((account) => (
          <AccountRow
            key={account.platform}
            account={account}
            onConnect={handleConnect}
          />
        ))}
      </div>
    </section>
  )
}

const notificationItems = [
  {
    key: 'uploadComplete',
    label: '上传完成',
    description: '视频处理完成后发送通知',
  },
  {
    key: 'promotionSuccess',
    label: '推广成功',
    description: '达到观看目标时发送提醒',
  },
  {
    key: 'systemUpdates',
    label: '系统更新',
    description: '新功能和维护公告',
  },
]

function NotificationSection() {
  const { data: preferences, isLoading } = useNotifications()
  const updateNotifications = useUpdateNotifications()

  const handleToggle = useCallback((key: string, value: boolean) => {
    updateNotifications.mutate({ [key]: value })
  }, [updateNotifications])

  if (isLoading) {
    return (
      <section className="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
        <SectionTitle title="通知偏好" icon="notifications_active" />
        <div className="space-y-6">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-12 bg-surface-container-high rounded animate-pulse" />
          ))}
        </div>
      </section>
    )
  }

  const defaultPreferences = {
    uploadComplete: true,
    promotionSuccess: true,
    systemUpdates: false,
  }

  const currentPreferences = preferences ?? defaultPreferences

  return (
    <section className="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
      <SectionTitle title="通知偏好" icon="notifications_active" />
      <div className="space-y-6">
        {notificationItems.map((item) => (
          <div key={item.key} className="flex items-center justify-between">
            <div>
              <p className="font-bold text-sm font-body">{item.label}</p>
              <p className="text-xs text-secondary font-body mt-0.5">{item.description}</p>
            </div>
            <Toggle
              checked={currentPreferences[item.key as keyof typeof currentPreferences]}
              onChange={(value) => handleToggle(item.key, value)}
            />
          </div>
        ))}
      </div>
    </section>
  )
}

interface ApiKeyCardProps {
  apiKey: ApiKey | undefined
  newKey: ApiKeyWithSecret | null
  onRegenerate: () => void
  onCopy: (text: string) => void
  isRegenerating: boolean
}

function ApiKeyCard({ apiKey, newKey, onRegenerate, onCopy, isRegenerating }: ApiKeyCardProps) {
  const [isRevealed, setIsRevealed] = useState(false)

  const displayKey = newKey
    ? (isRevealed ? newKey.key : `••••••••••••${newKey.prefix.slice(-4)}`)
    : apiKey
      ? (isRevealed ? apiKey.prefix : `••••••••••••${apiKey.prefix.slice(-4)}`)
      : '••••••••••••••••'

  const copyText = newKey?.key ?? apiKey?.prefix ?? ''

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return '未知'
    return new Date(dateStr).toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  return (
    <div className="bg-surface p-4 rounded-lg border-l-4 border-primary mb-6">
      <p className="text-xs font-bold text-primary uppercase tracking-widest mb-1">
        Primary API Key
      </p>
      <div className="flex items-center justify-between">
        <code className="text-lg font-mono tracking-tighter text-on-surface-variant">
          {displayKey}
        </code>
        <div className="flex gap-2">
          <button
            className="p-2 hover:bg-surface-container-high rounded-md transition-colors"
            title="复制密钥"
            onClick={() => onCopy(copyText)}
            disabled={!copyText}
          >
            <Icon name="content_copy" size={18} />
          </button>
          <button
            className="p-2 hover:bg-surface-container-high rounded-md transition-colors"
            title={isRevealed ? '隐藏' : '显示'}
            onClick={() => setIsRevealed(!isRevealed)}
          >
            <Icon name={isRevealed ? 'visibility_off' : 'visibility'} size={18} />
          </button>
        </div>
      </div>
      {(apiKey ?? newKey) && (
        <div className="mt-4 grid grid-cols-2 gap-4 text-xs text-on-surface-variant">
          <div>
            <span className="font-medium">过期时间：</span>
            {formatDate((newKey?.expiresAt ?? apiKey?.expiresAt) || null)}
          </div>
          <div>
            <span className="font-medium">最后使用：</span>
            {formatDate((newKey?.lastUsedAt ?? apiKey?.lastUsedAt) || null)}
          </div>
        </div>
      )}
      <button
        className="mt-4 w-full bg-surface-container-high text-on-surface-variant font-bold py-3 rounded-lg hover:bg-surface-variant transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
        onClick={onRegenerate}
        disabled={isRegenerating}
      >
        <Icon name="refresh" size={18} />
        {isRegenerating ? '重新生成中...' : '重新生成 API Key'}
      </button>
      {(apiKey ?? newKey) && (
        <p className="text-[10px] text-secondary mt-4 text-center">
          最后更新：{formatDate((newKey?.createdAt ?? apiKey?.createdAt) || null)}
        </p>
      )}
    </div>
  )
}

function ProTipCard() {
  return (
    <div className="bg-tertiary-fixed p-6 rounded-xl relative overflow-hidden group">
      <div className="relative z-10">
        <h4 className="font-headline font-extrabold text-on-tertiary-fixed mb-2">
          Pro Curator Tip
        </h4>
        <p className="text-xs text-on-tertiary-fixed-variant leading-relaxed font-body">
          连接所有账户后，Grace 会自动同步发布计划并根据时区优化互动时间。
        </p>
      </div>
      <Icon
        name="tips_and_updates"
        size={64}
        className="absolute -bottom-4 -right-4 text-on-tertiary-fixed/10 group-hover:scale-110 transition-transform duration-700"
      />
    </div>
  )
}

function ApiKeySection() {
  const { data: apiKeys, isLoading } = useApiKeys()
  const createApiKey = useCreateApiKey()
  const deleteApiKey = useDeleteApiKey()
  const [newKey, setNewKey] = useState<ApiKeyWithSecret | null>(null)
  const [showConfirmDialog, setShowConfirmDialog] = useState(false)

  const handleCopy = useCallback(async (text: string) => {
    if (!text) return
    try {
      await navigator.clipboard.writeText(text)
      alert('已复制到剪贴板')
    } catch {
      alert('复制失败')
    }
  }, [])

  const handleRegenerate = useCallback(() => {
    setShowConfirmDialog(true)
  }, [])

  const confirmRegenerate = useCallback(() => {
    setShowConfirmDialog(false)
    const currentKeyId = apiKeys?.[0]?.apiKeyId
    if (currentKeyId) {
      deleteApiKey.mutate(currentKeyId, {
        onSuccess: () => {
          createApiKey.mutate(
            { name: 'Primary Key', expiresInDays: 365 },
            {
              onSuccess: (result) => {
                setNewKey(result)
              },
            }
          )
        },
      })
    } else {
      createApiKey.mutate(
        { name: 'Primary Key', expiresInDays: 365 },
        {
          onSuccess: (result) => {
            setNewKey(result)
          },
        }
      )
    }
  }, [apiKeys, deleteApiKey, createApiKey])

  if (isLoading) {
    return (
      <section className="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
        <SectionTitle title="API 管理" icon="key" />
        <div className="h-32 bg-surface rounded-lg animate-pulse" />
      </section>
    )
  }

  const primaryApiKey = apiKeys?.[0]

  return (
    <section className="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
      <SectionTitle title="API 管理" icon="key" />
      <ApiKeyCard
        apiKey={primaryApiKey}
        newKey={newKey}
        onRegenerate={handleRegenerate}
        onCopy={handleCopy}
        isRegenerating={deleteApiKey.isPending || createApiKey.isPending}
      />
      {newKey && (
        <div className="mt-4 p-3 bg-tertiary-container/10 rounded-lg border border-tertiary/20">
          <p className="text-xs font-medium text-tertiary">
            新 API Key 已生成。请立即复制保存，此密钥仅显示一次。
          </p>
        </div>
      )}
      {showConfirmDialog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-surface-container-lowest rounded-xl p-6 max-w-sm shadow-lg">
            <h4 className="font-headline font-bold text-on-surface mb-4">确认重新生成</h4>
            <p className="text-sm text-on-surface-variant mb-6">
              重新生成将删除当前 API Key，使用旧 Key 的应用将无法继续访问。是否继续？
            </p>
            <div className="flex gap-3 justify-end">
              <Button variant="secondary" onClick={() => setShowConfirmDialog(false)}>
                取消
              </Button>
              <Button variant="danger" onClick={confirmRegenerate}>
                确认重新生成
              </Button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}

export default function SettingsPage() {
  return (
    <div className="px-8 py-6">
      <div className="mb-10">
        <h1 className="font-headline text-4xl font-extrabold tracking-tight text-on-surface mb-2">
          设置
        </h1>
        <p className="font-body text-on-surface-variant">
          管理您的用户资料、已连接账户、通知偏好和 API Key。
        </p>
      </div>

      <div className="grid grid-cols-12 gap-8">
        <div className="col-span-12 lg:col-span-7 flex flex-col gap-8">
          <ProfileSection />
          <ConnectedAccountsSection />
        </div>

        <div className="col-span-12 lg:col-span-5 flex flex-col gap-8">
          <NotificationSection />
          <ApiKeySection />
          <ProTipCard />
        </div>
      </div>
    </div>
  )
}