import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getProfile,
  updateProfile,
  uploadAvatar,
  getConnectedAccounts,
  getNotifications,
  updateNotifications,
  getApiKeys,
  createApiKey,
  deleteApiKey,
} from '@/api/settings'
import type { UpdateProfileRequest, UpdateNotificationsRequest, CreateApiKeyRequest } from '@/types/settings'

// Query key constants
export const settingsKeys = {
  all: ['settings'] as const,
  profile: () => [...settingsKeys.all, 'profile'] as const,
  connectedAccounts: () => [...settingsKeys.all, 'connectedAccounts'] as const,
  notifications: () => [...settingsKeys.all, 'notifications'] as const,
  apiKeys: () => [...settingsKeys.all, 'apiKeys'] as const,
}

// Profile query
export function useProfile() {
  return useQuery({
    queryKey: settingsKeys.profile(),
    queryFn: () => getProfile(),
  })
}

// Update profile mutation
export function useUpdateProfile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: UpdateProfileRequest) => updateProfile(request),
    onSuccess: (data) => {
      // Update cache with new profile data
      queryClient.setQueryData(settingsKeys.profile(), data)
    },
  })
}

// Upload avatar mutation
export function useUploadAvatar() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (avatar: File) => uploadAvatar(avatar),
    onSuccess: () => {
      // Invalidate profile to get updated avatar URL
      queryClient.invalidateQueries({ queryKey: settingsKeys.profile() })
    },
  })
}

// Connected accounts query
export function useConnectedAccounts() {
  return useQuery({
    queryKey: settingsKeys.connectedAccounts(),
    queryFn: () => getConnectedAccounts(),
  })
}

// Notifications query
export function useNotifications() {
  return useQuery({
    queryKey: settingsKeys.notifications(),
    queryFn: () => getNotifications(),
  })
}

// Update notifications mutation
export function useUpdateNotifications() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: UpdateNotificationsRequest) => updateNotifications(request),
    onSuccess: (data) => {
      // Update cache with new notification preferences
      queryClient.setQueryData(settingsKeys.notifications(), data)
    },
  })
}

// API keys query
export function useApiKeys() {
  return useQuery({
    queryKey: settingsKeys.apiKeys(),
    queryFn: () => getApiKeys(),
  })
}

// Create API key mutation
export function useCreateApiKey() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CreateApiKeyRequest) => createApiKey(request),
    onSuccess: () => {
      // Invalidate API keys list
      queryClient.invalidateQueries({ queryKey: settingsKeys.apiKeys() })
    },
  })
}

// Delete API key mutation
export function useDeleteApiKey() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (apiKeyId: string) => deleteApiKey(apiKeyId),
    onSuccess: () => {
      // Invalidate API keys list
      queryClient.invalidateQueries({ queryKey: settingsKeys.apiKeys() })
    },
  })
}
